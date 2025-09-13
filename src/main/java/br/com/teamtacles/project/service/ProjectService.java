package br.com.teamtacles.project.service;

import br.com.teamtacles.common.dto.response.InviteLinkResponseDTO;
import br.com.teamtacles.common.dto.response.page.PagedResponse;
import br.com.teamtacles.common.exception.ResourceAlreadyExistsException;
import br.com.teamtacles.common.exception.ResourceNotFoundException;
import br.com.teamtacles.common.mapper.PagedResponseMapper;
import br.com.teamtacles.common.service.EmailService;
import br.com.teamtacles.project.dto.request.InviteProjectMemberRequestDTO;
import br.com.teamtacles.project.dto.request.ProjectRequestUpdateDTO;
import br.com.teamtacles.project.dto.request.ProjectRequestRegisterDTO;
import br.com.teamtacles.project.dto.request.UpdateMemberRoleProjectRequestDTO;
import br.com.teamtacles.project.dto.response.ProjectMemberResponseDTO;
import br.com.teamtacles.project.dto.response.ProjectResponseDTO;
import br.com.teamtacles.project.enumeration.EProjectRole;
import br.com.teamtacles.project.model.Project;
import br.com.teamtacles.project.model.ProjectMember;
import br.com.teamtacles.project.repository.ProjectMemberRepository;
import br.com.teamtacles.project.repository.ProjectRepository;
import br.com.teamtacles.team.dto.request.UpdateMemberRoleTeamRequestDTO;
import br.com.teamtacles.team.enumeration.ETeamRole;
import br.com.teamtacles.user.model.User;
import br.com.teamtacles.user.repository.UserRepository; // Import necessário
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.util.UUID;



@Service
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

    @Value("${app.base-url}")
    private String baseUrl;

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ModelMapper modelMapper;
    private final ProjectAuthorizationService projectAuthorizationService;
    private final PagedResponseMapper pagedResponseMapper;
    private final UserRepository userRepository;
    private  final EmailService emailService;

    public ProjectService(ProjectRepository projectRepository, ProjectMemberRepository projectMemberRepository, ModelMapper modelMapper, ProjectAuthorizationService projectAuthorizationService, PagedResponseMapper pagedResponseMapper, UserRepository userRepository, EmailService emailService) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.modelMapper = modelMapper;
        this.projectAuthorizationService = projectAuthorizationService;
        this.pagedResponseMapper = pagedResponseMapper;
        this.userRepository = userRepository;
        this.emailService = emailService;

    }

    @Transactional
    public ProjectResponseDTO createProject(ProjectRequestRegisterDTO requestDTO, User actingUser) {
        log.info("User {} is attempting to create a new project with title: {}", actingUser.getUsername(), requestDTO.getTitle());
        validateProjectNameUniqueness(requestDTO.getTitle(), actingUser);

        Project newProject = modelMapper.map(requestDTO, Project.class);
        newProject.setOwner(actingUser);

        ProjectMember creatorMembership = new ProjectMember(actingUser, newProject, EProjectRole.OWNER);
        creatorMembership.setAcceptedInvite(true);
        newProject.getMembers().add(creatorMembership);

        Project savedProject = projectRepository.save(newProject);
        log.info("Project '{}' created successfully with ID: {} for user {}", savedProject.getTitle(), savedProject.getId(), actingUser.getUsername());
        return modelMapper.map(savedProject, ProjectResponseDTO.class);
    }

    @Transactional
    public ProjectResponseDTO updateProject(Long projectId, ProjectRequestUpdateDTO requestDTO, User actingUser) {
        log.info("User {} is attempting to update project ID: {}", actingUser.getUsername(), projectId);
        Project project = findProjectByIdOrThrow(projectId);

        projectAuthorizationService.checkProjectAdmin(actingUser, project);

        if (requestDTO.getTitle() != null && !requestDTO.getTitle().equalsIgnoreCase(project.getTitle())) {
            log.debug("Updating project title from '{}' to '{}'", project.getTitle(), requestDTO.getTitle());
            validateProjectNameUniqueness(requestDTO.getTitle(), project.getOwner());
        }

        if (requestDTO.getTitle() != null && !requestDTO.getTitle().isBlank()) {
            project.setTitle(requestDTO.getTitle());
        }

        if (requestDTO.getDescription() != null) {
            log.debug("Updating project description for project ID: {}", projectId);
            project.setDescription(requestDTO.getDescription());
        }

        Project updatedProject = projectRepository.save(project);
        log.info("Project ID: {} updated successfully by user {}", projectId, actingUser.getUsername());
        return modelMapper.map(updatedProject, ProjectResponseDTO.class);
    }

    @Transactional
    public ProjectMemberResponseDTO updateMemberRole(Long projectId, Long userIdToUpdate, UpdateMemberRoleProjectRequestDTO dto, User actingUser) {
        log.info("User {} is attempting to update role for user {} in project {}. New role: {}",
            actingUser.getUsername(), userIdToUpdate, projectId, dto.getNewRole());

        Project project = findProjectByIdOrThrow(projectId);
        projectAuthorizationService.checkProjectAdmin(actingUser, project);

        User userToUpdate = findUserByIdOrThrow(userIdToUpdate);
        ProjectMember membershipToUpdate = findMembershipByIdOrThrow(userToUpdate, project);

        if (membershipToUpdate.getProjectRole().equals(EProjectRole.OWNER)) {
            log.warn("Attempt to change OWNER role denied. Acting user: {}, Target user: {}",
                actingUser.getUsername(), userToUpdate.getUsername());
            throw new AccessDeniedException("The team OWNER's role cannot be changed.");
        }

        if (membershipToUpdate.getProjectRole().equals(EProjectRole.ADMIN)) {
            log.debug("Attempting to change ADMIN role, checking if acting user is OWNER");
            projectAuthorizationService.checkProjectOwner(actingUser, project);
        }

        if (dto.getNewRole().equals(EProjectRole.OWNER)) {
            log.warn("Attempt to promote user to OWNER role denied. Acting user: {}, Target user: {}",
                actingUser.getUsername(), userToUpdate.getUsername());
            throw new IllegalArgumentException("Cannot promote a user to OWNER.");
        }

        log.debug("Updating role from {} to {} for user {} in project {}",
            membershipToUpdate.getProjectRole(), dto.getNewRole(), userToUpdate.getUsername(), projectId);
        membershipToUpdate.setProjectRole(dto.getNewRole());
        ProjectMember updatedMembership = projectMemberRepository.save(membershipToUpdate);

        log.info("Role successfully updated for user {} to {} in project {}",
            userToUpdate.getUsername(), dto.getNewRole(), projectId);
        return toProjectMemberResponseDTO(updatedMembership);
    }

    public ProjectResponseDTO getProjectById(Long projectId, User actingUser) {
        log.debug("User {} requesting project with ID: {}", actingUser.getUsername(), projectId);
        Project project = findProjectByIdOrThrow(projectId);
        projectAuthorizationService.checkProjectMembership(actingUser, project);
        log.debug("Project {} ({}) successfully accessed by user {}", project.getTitle(), projectId, actingUser.getUsername());
        return modelMapper.map(project, ProjectResponseDTO.class);
    }

    public PagedResponse<ProjectResponseDTO> getAllProjectsByUser(Pageable pageable, User actingUser) {
        log.debug("Fetching projects for user {}. Page: {}, Size: {}",
            actingUser.getUsername(), pageable.getPageNumber(), pageable.getPageSize());

        Page<ProjectMember> userProjectsPage = projectMemberRepository.findByUserAndAcceptedInviteTrue(actingUser, pageable);

        Page<ProjectResponseDTO> projectResponseDTOPage = userProjectsPage.map(membership ->
                modelMapper.map(membership.getProject(), ProjectResponseDTO.class)
        );

        log.debug("Found {} projects for user {}", userProjectsPage.getTotalElements(), actingUser.getUsername());
        return pagedResponseMapper.toPagedResponse(projectResponseDTOPage, ProjectResponseDTO.class);
    }

    public PagedResponse<ProjectMemberResponseDTO> getAllMembersFromProject(Pageable pageable, Long projectId, User actingUser) {
        log.debug("User {} requesting members for project {}. Page: {}, Size: {}",
            actingUser.getUsername(), projectId, pageable.getPageNumber(), pageable.getPageSize());

        Project project = findProjectByIdOrThrow(projectId);
        projectAuthorizationService.checkProjectMembership(actingUser, project);

        Page<ProjectMember> projectMembersPage = projectMemberRepository.findByProjectAndAcceptedInviteTrue(project, pageable);
        Page<ProjectMemberResponseDTO> projectMemberResponseDTOPage = projectMembersPage.map(this::toProjectMemberResponseDTO);

        log.debug("Found {} members for project {}", projectMembersPage.getTotalElements(), projectId);
        return pagedResponseMapper.toPagedResponse(projectMemberResponseDTOPage, ProjectMemberResponseDTO.class);
    }

    @Transactional
    public void deleteProject(Long projectId, User actingUser) {
        log.info("User {} attempting to delete project {}", actingUser.getUsername(), projectId);
        Project project = findProjectByIdOrThrow(projectId);
        projectAuthorizationService.checkProjectOwner(actingUser, project);

        log.debug("Deleting project: {}. Title: {}", projectId, project.getTitle());
        projectRepository.delete(project);
        log.info("Project {} successfully deleted by user {}", projectId, actingUser.getUsername());
    }

    @Transactional
    public void deleteMembershipFromProject(Long projectId, Long userIdToDelete, User actingUser) {
        log.info("User {} attempting to remove user {} from project {}", actingUser.getUsername(), userIdToDelete, projectId);
        Project project = findProjectByIdOrThrow(projectId);
        projectAuthorizationService.checkProjectAdmin(actingUser, project);

        User userToDelete = findUserByIdOrThrow(userIdToDelete);
        ProjectMember membershipToDelete = findMembershipByIdOrThrow(userToDelete, project);
        ProjectMember actingMembership = findMembershipByIdOrThrow(actingUser, project);

        boolean actingUserIsOwner = actingMembership.getProjectRole().equals(EProjectRole.OWNER);
        boolean targetIsPrivileged = membershipToDelete.getProjectRole().isPrivileged();

        if (actingUserIsOwner && membershipToDelete.equals(actingMembership)) {
            log.warn("Owner {} attempted to remove themselves from project {}", actingUser.getUsername(), projectId);
            throw new AccessDeniedException("OWNER cannot remove themselves.");
        }

        if (!actingUserIsOwner && targetIsPrivileged) {
            log.warn("Non-owner user {} attempted to remove privileged member {} from project {}",
                actingUser.getUsername(), userToDelete.getUsername(), projectId);
            throw new AccessDeniedException("You cannot remove a member with role OWNER or ADMIN.");
        }

        log.debug("Removing member {} (role: {}) from project {}",
            userToDelete.getUsername(), membershipToDelete.getProjectRole(), projectId);
        projectMemberRepository.delete(membershipToDelete);
        log.info("User {} successfully removed from project {} by {}",
            userToDelete.getUsername(), projectId, actingUser.getUsername());
    }

    @Transactional
    public void inviteMember(Long projectId, InviteProjectMemberRequestDTO requestDTO, User actingUser) {
        log.info("User {} is attempting to invite user with email {} to project ID: {}", actingUser.getUsername(), requestDTO.getEmail(), projectId);
        Project project = findProjectByIdOrThrow(projectId);
        projectAuthorizationService.checkProjectAdmin(actingUser, project);

        if (requestDTO.getRole().equals(EProjectRole.OWNER)) {
            throw new IllegalArgumentException("Cannot invite a user to be an OWNER.");
        }

        User userToInvite = findByEmailIgnoreCaseOrThrow(requestDTO.getEmail());

        if (projectMemberRepository.findByUserAndProject(userToInvite, project).isPresent()) {
            log.warn("Invite failed: User {} is already a member of project ID: {}", userToInvite.getUsername(), projectId);
            throw new ResourceAlreadyExistsException("User is already a member of this project.");
        }

        ProjectMember newMember = new ProjectMember(userToInvite, project, requestDTO.getRole());
        newMember.setInvitationToken(UUID.randomUUID().toString());
        newMember.setInvitationTokenExpiry(LocalDateTime.now().plusHours(24));
        newMember.setAcceptedInvite(false);

        projectMemberRepository.save(newMember);
        log.info("User {} invited successfully to project ID: {}. Invitation token generated.", userToInvite.getUsername(), projectId);

        emailService.sendProjectInvitationEmail( userToInvite.getEmail(), project.getTitle(), newMember.getInvitationToken());
    }

    @Transactional
    public void acceptInvitation(String token) {
        log.info("Processing invitation acceptance with token: {}", token);

        if (token == null || token.isEmpty()) {
            log.warn("Attempt to accept invitation with null or empty token");
            throw new IllegalArgumentException("Invitation token cannot be null or empty.");
        }

        ProjectMember membership = findByInvitationTokenEmailOrThrow(token);

        if (membership.getInvitationTokenExpiry().isBefore(LocalDateTime.now())) {
            log.warn("Attempt to accept expired invitation token for project: {}", membership.getProject().getId());
            throw new ResourceNotFoundException("Invitation token has expired.");
        }

        log.debug("Accepting invitation for user {} in project {}",
            membership.getUser().getUsername(), membership.getProject().getId());

        membership.setAcceptedInvite(true);
        membership.setInvitationToken(null);
        membership.setInvitationTokenExpiry(null);

        projectMemberRepository.save(membership);
        log.info("Invitation accepted successfully for user {} in project {}",
            membership.getUser().getUsername(), membership.getProject().getId());
    }

    @Transactional
    public InviteLinkResponseDTO generateInvitedLink(Long projectId, User actingUser) {
        log.info("User {} requesting invitation link for project {}", actingUser.getUsername(), projectId);

        Project project = findProjectByIdOrThrow(projectId);
        projectAuthorizationService.checkProjectAdmin(actingUser, project);

        String token = UUID.randomUUID().toString();
        project.setInvitationToken(token);
        project.setInvitationTokenExpiry(LocalDateTime.now().plusHours(24));

        projectRepository.save(project);
        log.info("Invitation link generated for project {} by user {}. Expires: {}",
            projectId, actingUser.getUsername(), project.getInvitationTokenExpiry());

        return new InviteLinkResponseDTO(baseUrl + "/api/project/join?token=" + token,
            project.getInvitationTokenExpiry());
    }

    @Transactional
    public ProjectMemberResponseDTO acceptProjectInvitationLink(String token, User actingUser) {
        log.info("User {} attempting to accept project invitation with token: {}", actingUser.getUsername(), token);

        if (token == null || token.isEmpty()) {
            log.warn("Attempt to accept invitation with null or empty token by user: {}", actingUser.getUsername());
            throw new IllegalArgumentException("Invitation token cannot be null or empty.");
        }

        Project project = findByInvitationTokenLinkOrThrow(token);

        if(project.getInvitationTokenExpiry().isBefore(LocalDateTime.now())) {
            log.warn("User {} attempted to accept expired invitation for project: {}", actingUser.getUsername(), project.getId());
            throw new ResourceNotFoundException("Invitation token has expired.");
        }

        log.debug("Creating new membership for user {} in project {}", actingUser.getUsername(), project.getId());
        ProjectMember newMember = new ProjectMember(actingUser, project, EProjectRole.MEMBER);
        newMember.setAcceptedInvite(true);

        ProjectMember savedMember = projectMemberRepository.save(newMember);
        log.info("User {} successfully joined project {} via invitation link", actingUser.getUsername(), project.getId());

        return toProjectMemberResponseDTO(newMember);
    }

    private void validateProjectNameUniqueness(String title, User owner) {
        if (projectRepository.existsByTitleIgnoreCaseAndOwner(title, owner)) {
            throw new ResourceAlreadyExistsException("Project name already in use by this creator.");
        }
    }

    private Project findProjectByIdOrThrow(Long projectId) {
        return projectRepository.findById(projectId).orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));
    }

    private ProjectMember findByInvitationTokenEmailOrThrow(String token) {
        return projectMemberRepository.findByInvitationToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid invitation token."));
    }

    private User findByEmailIgnoreCaseOrThrow(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email"));
    }

    private Project findByInvitationTokenLinkOrThrow(String token) {
        return projectRepository.findByInvitationToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid invitation."));
    }

    private User findUserByIdOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    private ProjectMember findMembershipByIdOrThrow(User user, Project project) {
        return projectMemberRepository.findByUserAndProject(user, project)
                .orElseThrow(() -> new ResourceNotFoundException("User to update not found in this team."));
    }

    private ProjectMemberResponseDTO toProjectMemberResponseDTO(ProjectMember membership) {
        ProjectMemberResponseDTO dto = new ProjectMemberResponseDTO();
        dto.setUserId(membership.getUser().getId());
        dto.setUsername(membership.getUser().getUsername());
        dto.setEmail(membership.getUser().getEmail());
        dto.setProjectRole(membership.getProjectRole());
        return dto;
    }
}


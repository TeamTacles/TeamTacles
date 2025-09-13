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

import java.time.LocalDateTime;
import java.util.UUID;



@Service
public class ProjectService {

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
        validateProjectNameUniqueness(requestDTO.getTitle(), actingUser);

        Project newProject = modelMapper.map(requestDTO, Project.class);
        newProject.setOwner(actingUser);

        ProjectMember creatorMembership = new ProjectMember(actingUser, newProject, EProjectRole.OWNER);
        creatorMembership.setAcceptedInvite(true);
        newProject.getMembers().add(creatorMembership);

        Project savedProject = projectRepository.save(newProject);
        return modelMapper.map(savedProject, ProjectResponseDTO.class);
    }

    @Transactional
    public ProjectResponseDTO updateProject(Long projectId, ProjectRequestUpdateDTO requestDTO, User actingUser) {
        Project project = findProjectByIdOrThrow(projectId);

        projectAuthorizationService.checkProjectAdmin(actingUser, project);

        if (requestDTO.getTitle() != null && !requestDTO.getTitle().equalsIgnoreCase(project.getTitle())) {
            validateProjectNameUniqueness(requestDTO.getTitle(), project.getOwner());
        }

        if (requestDTO.getTitle() != null && !requestDTO.getTitle().isBlank()) {
            project.setTitle(requestDTO.getTitle());
        }

        if (requestDTO.getDescription() != null) {
            project.setDescription(requestDTO.getDescription());
        }

        Project updatedProject = projectRepository.save(project);
        return modelMapper.map(updatedProject, ProjectResponseDTO.class);
    }

    @Transactional
    public ProjectMemberResponseDTO updateMemberRole(Long projectId, Long userIdToUpdate, UpdateMemberRoleProjectRequestDTO dto, User actingUser) {
        Project project = findProjectByIdOrThrow(projectId);
        projectAuthorizationService.checkProjectAdmin(actingUser, project);

        User userToUpdate = findUserByIdOrThrow(userIdToUpdate);
        ProjectMember membershipToUpdate = findMembershipByIdOrThrow(userToUpdate, project);

        if (membershipToUpdate.getProjectRole().equals(EProjectRole.OWNER)) {
            throw new AccessDeniedException("The team OWNER's role cannot be changed.");
        }

        if (membershipToUpdate.getProjectRole().equals(EProjectRole.ADMIN)) {
            projectAuthorizationService.checkProjectOwner(actingUser, project);
        }

        if (dto.getNewRole().equals(EProjectRole.OWNER)) {
            throw new IllegalArgumentException("Cannot promote a user to OWNER.");
        }

        membershipToUpdate.setProjectRole(dto.getNewRole());
        ProjectMember updatedMembership = projectMemberRepository.save(membershipToUpdate);

        return toProjectMemberResponseDTO(updatedMembership);
    }

    public ProjectResponseDTO getProjectById(Long projectId, User actingUser) {
        Project project = findProjectByIdOrThrow(projectId);
        projectAuthorizationService.checkProjectMembership(actingUser, project);
        return modelMapper.map(project, ProjectResponseDTO.class);
    }

    public PagedResponse<ProjectResponseDTO> getAllProjectsByUser(Pageable pageable, User actingUser) {
        Page<ProjectMember> userProjectsPage = projectMemberRepository.findByUserAndAcceptedInviteTrue(actingUser, pageable);

        Page<ProjectResponseDTO> projectResponseDTOPage = userProjectsPage.map(membership ->
                modelMapper.map(membership.getProject(), ProjectResponseDTO.class)
        );
        return pagedResponseMapper.toPagedResponse(projectResponseDTOPage, ProjectResponseDTO.class);
    }

    public PagedResponse<ProjectMemberResponseDTO> getAllMembersFromProject(Pageable pageable, Long projectId, User actingUser) {
        Project project = findProjectByIdOrThrow(projectId);
        projectAuthorizationService.checkProjectMembership(actingUser, project);

        Page<ProjectMember> projectMembersPage = projectMemberRepository.findByProjectAndAcceptedInviteTrue(project, pageable);

        Page<ProjectMemberResponseDTO> projectMemberResponseDTOPage = projectMembersPage.map(this::toProjectMemberResponseDTO);

        return pagedResponseMapper.toPagedResponse(projectMemberResponseDTOPage, ProjectMemberResponseDTO.class);
    }

    @Transactional
    public void deleteProject(Long projectId, User actingUser) {
        Project project = findProjectByIdOrThrow(projectId);
        projectAuthorizationService.checkProjectOwner(actingUser, project); // Only owner can delete the project
        projectRepository.delete(project);
    }

    @Transactional
    public void deleteMembershipFromProject(Long projectId, Long userIdToDelete, User actingUser) {
        Project project = findProjectByIdOrThrow(projectId);
        projectAuthorizationService.checkProjectAdmin(actingUser, project);

        User userToDelete = findUserByIdOrThrow(userIdToDelete);
        ProjectMember membershipToDelete = findMembershipByIdOrThrow(userToDelete, project);
        ProjectMember actingMembership = findMembershipByIdOrThrow(actingUser, project);

        boolean actingUserIsOwner = actingMembership.getProjectRole().equals(EProjectRole.OWNER);
        boolean targetIsPrivileged = membershipToDelete.getProjectRole().isPrivileged();

        if (actingUserIsOwner && membershipToDelete.equals(actingMembership)) {
            throw new AccessDeniedException("OWNER cannot remove themselves.");
        }

        if (!actingUserIsOwner && targetIsPrivileged) {
            throw new AccessDeniedException("You cannot remove a member with role OWNER or ADMIN.");
        }

        projectMemberRepository.delete(membershipToDelete);
    }

    @Transactional
    public void inviteMember(Long projectId, InviteProjectMemberRequestDTO requestDTO, User actingUser) {
        Project project = findProjectByIdOrThrow(projectId);
        projectAuthorizationService.checkProjectAdmin(actingUser, project);

        if (requestDTO.getRole().equals(EProjectRole.OWNER)) {
            throw new IllegalArgumentException("Cannot invite a user to be an OWNER.");
        }

        User userToInvite = findByEmailIgnoreCaseOrThrow(requestDTO.getEmail());

        if (projectMemberRepository.findByUserAndProject(userToInvite, project).isPresent()) {
            throw new ResourceAlreadyExistsException("User is already a member of this project.");
        }

        ProjectMember newMember = new ProjectMember(userToInvite, project, requestDTO.getRole());
        newMember.setInvitationToken(UUID.randomUUID().toString());
        newMember.setInvitationTokenExpiry(LocalDateTime.now().plusHours(24));
        newMember.setAcceptedInvite(false);

        projectMemberRepository.save(newMember);

        emailService.sendProjectInvitationEmail( userToInvite.getEmail(), project.getTitle(), newMember.getInvitationToken());
    }

    @Transactional
    public void acceptInvitation(String token) {
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("Invitation token cannot be null or empty.");
        }

        ProjectMember membership = findByInvitationTokenEmailOrThrow(token);

        if (membership.getInvitationTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new ResourceNotFoundException("Invitation token has expired.");
        }

        membership.setAcceptedInvite(true);
        membership.setInvitationToken(null);
        membership.setInvitationTokenExpiry(null);

        projectMemberRepository.save(membership);
    }

    @Transactional
    public InviteLinkResponseDTO generateInvitedLink(Long projectId, User actingUser) {
        Project project = findProjectByIdOrThrow(projectId);
        projectAuthorizationService.checkProjectAdmin(actingUser, project);

        String token = UUID.randomUUID().toString();
        project.setInvitationToken(token);
        project.setInvitationTokenExpiry(LocalDateTime.now().plusHours(24));

        projectRepository.save(project);

        return new InviteLinkResponseDTO(baseUrl + "/api/project/join?token=" + token, project.getInvitationTokenExpiry());
    }

    @Transactional
    public ProjectMemberResponseDTO acceptProjectInvitationLink(String token, User actingUser) {
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("Invitation token cannot be null or empty.");
        }

        Project project = findByInvitationTokenLinkOrThrow(token);

        if(project.getInvitationTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new ResourceNotFoundException("Invitation token has expired.");
        }

        ProjectMember newMember = new ProjectMember(actingUser, project, EProjectRole.MEMBER);
        newMember.setAcceptedInvite(true);

        ProjectMember savedMember = projectMemberRepository.save(newMember);

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



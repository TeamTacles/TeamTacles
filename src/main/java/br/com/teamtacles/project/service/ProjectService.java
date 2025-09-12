package br.com.teamtacles.project.service;

import br.com.teamtacles.common.dto.page.PagedResponse;
import br.com.teamtacles.common.exception.ResourceAlreadyExistsException;
import br.com.teamtacles.common.exception.ResourceNotFoundException;
import br.com.teamtacles.common.mapper.PagedResponseMapper;
import br.com.teamtacles.common.service.EmailService;
import br.com.teamtacles.project.dto.request.InviteProjectMemberRequestDTO;
import br.com.teamtacles.project.dto.request.ProjectRequestUpdateDTO;
import br.com.teamtacles.project.dto.request.ProjectRequestRegisterDTO;
import br.com.teamtacles.project.dto.response.ProjectResponseDTO;
import br.com.teamtacles.project.enumeration.EProjectRole;
import br.com.teamtacles.project.model.Project;
import br.com.teamtacles.project.model.ProjectMember;
import br.com.teamtacles.project.repository.ProjectMemberRepository;
import br.com.teamtacles.project.repository.ProjectRepository;
import br.com.teamtacles.user.model.User;
import br.com.teamtacles.user.repository.UserRepository; // Import necessário
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;



@Service
public class ProjectService {

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

    private void validateProjectNameUniqueness(String title, User owner) {
        if (projectRepository.existsByTitleIgnoreCaseAndOwner(title, owner)) {
            throw new ResourceAlreadyExistsException("Project name already in use by this creator.");
        }
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
    public void deleteProject(Long projectId, User actingUser) {
        Project project = findProjectByIdOrThrow(projectId);
        projectAuthorizationService.checkProjectOwner(actingUser, project); // Only owner can delete the project
        projectRepository.delete(project);
    }

    @Transactional
    public void inviteMember(Long projectId, InviteProjectMemberRequestDTO requestDTO, User actingUser) {
        Project project = findProjectByIdOrThrow(projectId);

        projectAuthorizationService.checkProjectAdmin(actingUser, project);

        if (requestDTO.getRole().equals(EProjectRole.OWNER)) {
            throw new IllegalArgumentException("Cannot invite a user to be an OWNER.");
        }

        User userToInvite = userRepository.findByEmailIgnoreCase(requestDTO.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User to invite not found with email: " + requestDTO.getEmail()));

        if (projectMemberRepository.findByUserAndProject(userToInvite, project).isPresent()) {
            throw new ResourceAlreadyExistsException("User is already a member of this project.");
        }

        ProjectMember newMember = new ProjectMember(userToInvite, project, requestDTO.getRole());
        newMember.setInvitationToken(UUID.randomUUID().toString());
        newMember.setInvitationTokenExpiry(LocalDateTime.now().plusHours(24));
        newMember.setAcceptedInvite(false);

        projectMemberRepository.save(newMember);

        emailService.sendProjectInvitationEmail(
                userToInvite.getEmail(),
                project.getTitle(),
                newMember.getInvitationToken()

        );

    }



    //Métodos Auxiliares
    private Project findProjectByIdOrThrow(Long projectId) {
        return projectRepository.findById(projectId).orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));
    }

    @Transactional
    public void acceptInvitation(String token) {
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("Invitation token cannot be null or empty.");
        }

        ProjectMember membership = findByInvitationTokenOrThrow(token);

        if (membership.getInvitationTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new ResourceNotFoundException("Invitation token has expired.");
        }

        membership.setAcceptedInvite(true);
        membership.setInvitationToken(null);
        membership.setInvitationTokenExpiry(null);

        projectMemberRepository.save(membership);
    }

    private ProjectMember findByInvitationTokenOrThrow(String token) {
        return projectMemberRepository.findByInvitationToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid invitation token."));
    }
}



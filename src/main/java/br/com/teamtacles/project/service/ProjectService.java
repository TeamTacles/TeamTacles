package br.com.teamtacles.project.service;

import br.com.teamtacles.common.dto.response.InviteLinkResponseDTO;
import br.com.teamtacles.common.dto.response.page.PagedResponse;
import br.com.teamtacles.common.exception.ResourceNotFoundException;
import br.com.teamtacles.common.mapper.PagedResponseMapper;
import br.com.teamtacles.common.service.EmailService;
import br.com.teamtacles.project.dto.report.TaskSummary;
import br.com.teamtacles.project.dto.request.*;
import br.com.teamtacles.project.dto.response.DashboardSummaryDTO;
import br.com.teamtacles.project.dto.response.ProjectMemberResponseDTO;
import br.com.teamtacles.project.dto.response.ProjectResponseDTO;
import br.com.teamtacles.project.dto.response.UserProjectResponseDTO;
import br.com.teamtacles.project.enumeration.EProjectRole;
import br.com.teamtacles.project.model.Project;
import br.com.teamtacles.project.model.ProjectMember;
import br.com.teamtacles.project.repository.ProjectMemberRepository;
import br.com.teamtacles.project.repository.ProjectRepository;
import br.com.teamtacles.project.validator.*;
import br.com.teamtacles.task.enumeration.ETaskStatus;
import br.com.teamtacles.task.model.Task;
import br.com.teamtacles.team.model.Team;
import br.com.teamtacles.team.model.TeamMember;
import br.com.teamtacles.team.service.TeamAuthorizationService;
import br.com.teamtacles.team.service.TeamService;
import br.com.teamtacles.user.model.User;
import br.com.teamtacles.user.service.UserService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.time.OffsetDateTime;

@Service
public class ProjectService {

    @Value("${app.base-url}")
    private String baseUrl;

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectAuthorizationService projectAuthorizationService;
    private final ProjectTitleUniquenessValidator projectTitleUniquenessValidator;
    private final ProjectMembershipValidator projectMembershipValidator;
    private final ProjectTokenValidator projectTokenValidator;
    private final ProjectMembershipActionValidator projectMembershipActionValidator;
    private final ProjectInvitationValidator projectInvitationValidator;

    private final UserService userService;
    private final EmailService emailService;
    private final TeamService teamService;
    private final TeamAuthorizationService teamAuthorizationService;

    private final ModelMapper modelMapper;
    private final PagedResponseMapper pagedResponseMapper;

    public ProjectService(
            ProjectRepository projectRepository, ProjectMemberRepository projectMemberRepository,
            ModelMapper modelMapper, ProjectAuthorizationService projectAuthorizationService,
            PagedResponseMapper pagedResponseMapper, UserService userService,
            EmailService emailService, TeamService teamService,
            TeamAuthorizationService teamAuthorizationService,
            ProjectTitleUniquenessValidator projectTitleUniquenessValidator,
            ProjectMembershipValidator projectMembershipValidator,
            ProjectTokenValidator projectTokenValidator,
            ProjectMembershipActionValidator projectMembershipActionValidator,
            ProjectInvitationValidator projectInvitationValidator
            ) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.modelMapper = modelMapper;
        this.projectAuthorizationService = projectAuthorizationService;
        this.pagedResponseMapper = pagedResponseMapper;
        this.userService = userService;
        this.emailService = emailService;
        this.teamService = teamService;
        this.teamAuthorizationService = teamAuthorizationService;
        this.projectTitleUniquenessValidator = projectTitleUniquenessValidator;
        this.projectMembershipValidator = projectMembershipValidator;
        this.projectTokenValidator = projectTokenValidator;
        this.projectMembershipActionValidator = projectMembershipActionValidator;
        this.projectInvitationValidator = projectInvitationValidator;
    }

    @Transactional
    public ProjectResponseDTO createProject(ProjectRequestRegisterDTO requestDTO, User actingUser) {
        projectTitleUniquenessValidator.validate(requestDTO.getTitle(), actingUser);

        Project newProject = modelMapper.map(requestDTO, Project.class);
        newProject.setOwner(actingUser);

        ProjectMember creatorMembership = new ProjectMember(actingUser, newProject, EProjectRole.OWNER);
        creatorMembership.setAcceptedInvite(true);
        newProject.addMember(creatorMembership);

        Project savedProject = projectRepository.save(newProject);
        return modelMapper.map(savedProject, ProjectResponseDTO.class);
    }

    @Transactional
    public ProjectResponseDTO updateProject(Long projectId, ProjectRequestUpdateDTO requestDTO, User actingUser) {
        Project project = findProjectByIdOrThrow(projectId);
        projectAuthorizationService.checkProjectAdmin(actingUser, project);

        if (requestDTO.getTitle() != null && !requestDTO.getTitle().equalsIgnoreCase(project.getTitle())) {
            projectTitleUniquenessValidator.validate(requestDTO.getTitle(), project.getOwner());
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

        User userToUpdate = userService.findUserEntityById(userIdToUpdate);
        ProjectMember membershipToUpdate = findMembershipByIdOrThrow(userToUpdate, project);
        ProjectMember actingMembership = findMembershipByIdOrThrow(actingUser, project);

        projectMembershipActionValidator.validateRoleUpdate(actingMembership, membershipToUpdate, dto.getNewRole());

        membershipToUpdate.setProjectRole(dto.getNewRole());
        ProjectMember updatedMembership = projectMemberRepository.save(membershipToUpdate);

        return toProjectMemberResponseDTO(updatedMembership);
    }

    @Transactional
    public void importTeamMembersToProject(Long projectId, Long teamId, User actingUser) {
        Project project = findProjectByIdOrThrow(projectId);
        projectAuthorizationService.checkProjectAdmin(actingUser, project);

        Team teamToImport = teamService.findTeamEntityById(teamId);
        teamAuthorizationService.checkTeamMembership(actingUser, teamToImport);

        Set<TeamMember> teamMembers = teamToImport.getMembers();

        for (TeamMember teamMember : teamMembers) {
            boolean teamMemberAlreadyInProject = project.getMembers().stream()
                    .anyMatch(present -> present.getUser().equals(teamMember.getUser()));

            if(!teamMemberAlreadyInProject) {
                ProjectMember newProjectMembership = new ProjectMember(teamMember.getUser(), project, EProjectRole.MEMBER);
                project.addMember(newProjectMembership);
            }
        }

        projectRepository.save(project);
    }

    public ProjectResponseDTO getProjectById(Long projectId, User actingUser) {
        Project project = findProjectByIdOrThrow(projectId);
        projectAuthorizationService.checkProjectMembership(actingUser, project);
        return modelMapper.map(project, ProjectResponseDTO.class);
    }

    public PagedResponse<UserProjectResponseDTO> getAllProjectsByUser(Pageable pageable, ProjectFilterDTO filter, User actingUser) {
        Page<Project> projectsPage = projectRepository.findProjectsByUserWithFilters(actingUser, filter, pageable);

        Page<UserProjectResponseDTO> userProjectDTOPage = projectsPage.map(project -> {
            ProjectMember membership = project.getMembers().stream()
                    .filter(member -> member.getUser().getId().equals(actingUser.getId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Data inconsistency: Project " + project.getId() + " found without the corresponding member."));

            return toUserProjectResponseDTO(project, membership);
        });

        return pagedResponseMapper.toPagedResponse(userProjectDTOPage, UserProjectResponseDTO.class);
    }


    public PagedResponse<ProjectMemberResponseDTO> getAllMembersFromProject(Pageable pageable, Long projectId, User actingUser) {
        Project project = findProjectByIdOrThrow(projectId);
        projectAuthorizationService.checkProjectMembership(actingUser, project);

        Page<ProjectMember> projectMembersPage = projectMemberRepository.findByProjectAndAcceptedInviteTrue(project, pageable);
        Page<ProjectMemberResponseDTO> projectMemberResponseDTOPage = projectMembersPage.map(this::toProjectMemberResponseDTO);

        return pagedResponseMapper.toPagedResponse(projectMemberResponseDTOPage, ProjectMemberResponseDTO.class);
    }

    public Project getProjectWithMembersAndTasks(Long projectId, User actingUser) {
        Project project = findProjectWithMembersAndTasksOrThrow(projectId);
        projectAuthorizationService.checkProjectOwner(actingUser, project);
        return project;
    }

    @Transactional
    public void inviteMember(Long projectId, InviteProjectMemberRequestDTO requestDTO, User actingUser) {
        Project project = findProjectByIdOrThrow(projectId);
        projectAuthorizationService.checkProjectAdmin(actingUser, project);
        projectInvitationValidator.validateRole(requestDTO.getRole());

        User userToInvite = userService.findUserEntityByEmail(requestDTO.getEmail());

        projectMembershipValidator.validateNewMember(userToInvite, project);

        ProjectMember newMember = new ProjectMember(userToInvite, project, requestDTO.getRole());
        newMember.setInvitationToken(UUID.randomUUID().toString());
        newMember.setInvitationTokenExpiry(LocalDateTime.now().plusHours(24));
        newMember.setAcceptedInvite(false);

        project.addMember(newMember);
        projectRepository.save(project);

        emailService.sendProjectInvitationEmail(userToInvite.getEmail(), project.getTitle(), newMember.getInvitationToken());
    }

    @Transactional
    public void acceptInvitation(String token) {
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("Invitation token cannot be null or empty.");
        }

        ProjectMember membership = findByInvitationTokenEmailOrThrow(token);
        projectTokenValidator.validateInvitationEmailToken(membership);

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

        return new InviteLinkResponseDTO(baseUrl + "/api/project/join?token=" + token,
            project.getInvitationTokenExpiry());
    }

    @Transactional
    public ProjectMemberResponseDTO acceptProjectInvitationLink(String token, User actingUser) {
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("Invitation token cannot be null or empty.");
        }

        Project project = findByInvitationTokenLinkOrThrow(token);

        projectTokenValidator.validateInvitationLinkToken(project);
        projectMembershipValidator.validateNewMember(actingUser, project);

        ProjectMember newMember = new ProjectMember(actingUser, project, EProjectRole.MEMBER);
        newMember.setAcceptedInvite(true);

        project.addMember(newMember);
        projectRepository.save(project);

        return toProjectMemberResponseDTO(newMember);
    }

    @Transactional
    public void deleteProject(Long projectId, User actingUser) {
        Project project = findProjectByIdOrThrow(projectId);
        projectAuthorizationService.checkProjectOwner(actingUser, project);
        projectRepository.delete(project);
    }

    @Transactional
    public void deleteMembershipFromProject(Long projectId, Long userIdToDelete, User actingUser) {
        Project project = findProjectByIdOrThrow(projectId);
        projectAuthorizationService.checkProjectAdmin(actingUser, project);

        User userToDelete = userService.findUserEntityById(userIdToDelete);
        ProjectMember membershipToDelete = findMembershipByIdOrThrow(userToDelete, project);
        ProjectMember actingMembership = findMembershipByIdOrThrow(actingUser, project);

        projectMembershipActionValidator.validateDeletion(actingMembership, membershipToDelete);

        project.removeMember(membershipToDelete);
        projectRepository.save(project);
    }

    public Project findProjectEntityById(Long teamId) {
        return findProjectByIdOrThrow(teamId);
    }

    public Set<User> findProjectMembersFromIdList(Long projectId, List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new HashSet<>();
        }
        return projectMemberRepository.findProjectMembersAsUsers(projectId, userIds);
    }

    private Project findProjectWithMembersAndTasksOrThrow(Long projectId) {
        return projectRepository.findByIdWithMembersAndTasks(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));
    }

    private Project findProjectByIdOrThrow(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));
    }

    private ProjectMember findByInvitationTokenEmailOrThrow(String token) {
        return projectMemberRepository.findByInvitationToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid invitation token."));
    }

    private Project findByInvitationTokenLinkOrThrow(String token) {
        return projectRepository.findByInvitationToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid invitation."));
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

    private UserProjectResponseDTO toUserProjectResponseDTO(Project project, ProjectMember membership) {
        UserProjectResponseDTO dto = new UserProjectResponseDTO();
        dto.setId(project.getId());
        dto.setTitle(project.getTitle());
        dto.setDescription(project.getDescription());
        dto.setProjectRole(membership.getProjectRole());
        return dto;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryDTO getDashboard(Long projectId, User actingUser) { // <-- MUDANÇA AQUI
        Project project = findProjectWithMembersAndTasksOrThrow(projectId);
        projectAuthorizationService.checkProjectMembership(actingUser, project);
        return calculateDashboardSummary(project.getTasks());
    }

    // Função auxiliar
    private DashboardSummaryDTO calculateDashboardSummary(Set<Task> tasks) {
        long doneCount = 0;
        long inProgressCount = 0;
        long toDoCount = 0;
        long overdueCount = 0;

        // Pega o momento atual UMA VEZ pra performance
        OffsetDateTime now = OffsetDateTime.now();

        for (Task task : tasks) {
            switch (task.getStatus()) {
                case DONE:
                    doneCount++;
                    break;
                case IN_PROGRESS:
                    inProgressCount++;
                    break;
                case TO_DO:
                    toDoCount++;
                    break;
            }

            if (task.getStatus() != ETaskStatus.DONE &&
                    task.getDueDate() != null &&
                    task.getDueDate().isBefore(now)) {
                overdueCount++;
            }
        }
        long totalCount = tasks.size();

        return new DashboardSummaryDTO(totalCount, doneCount, inProgressCount, toDoCount, overdueCount);
    }


}


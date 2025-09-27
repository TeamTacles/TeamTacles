package br.com.teamtacles.project.service;

import br.com.teamtacles.common.dto.response.InviteLinkResponseDTO;
import br.com.teamtacles.common.dto.response.page.PagedResponse;
import br.com.teamtacles.common.exception.ResourceNotFoundException;
import br.com.teamtacles.common.mapper.PagedResponseMapper;
import br.com.teamtacles.infrastructure.email.EmailService;
import br.com.teamtacles.project.dto.request.*;
import br.com.teamtacles.project.dto.response.ProjectMemberResponseDTO;
import br.com.teamtacles.project.dto.response.ProjectReportDTO;
import br.com.teamtacles.project.dto.response.ProjectResponseDTO;
import br.com.teamtacles.project.dto.response.UserProjectResponseDTO;
import br.com.teamtacles.project.enumeration.EProjectRole;
import br.com.teamtacles.project.model.Project;
import br.com.teamtacles.project.model.ProjectMember;
import br.com.teamtacles.project.repository.ProjectMemberRepository;
import br.com.teamtacles.project.repository.ProjectRepository;
import br.com.teamtacles.project.validator.*;
import br.com.teamtacles.task.dto.request.TaskFilterReportDTO;
import br.com.teamtacles.task.dto.response.TaskSummaryDTO;
import br.com.teamtacles.task.enumeration.ETaskStatus;
import br.com.teamtacles.task.model.Task;
import br.com.teamtacles.task.repository.TaskRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import br.com.teamtacles.project.dto.response.MemberPerformanceDTO;
import br.com.teamtacles.project.dto.response.ProjectReportDTO;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import br.com.teamtacles.config.aop.BusinessActivityLog;

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
    private final TaskRepository taskRepository;

    public ProjectService(
            ProjectRepository projectRepository, ProjectMemberRepository projectMemberRepository,
            ModelMapper modelMapper, ProjectAuthorizationService projectAuthorizationService,
            PagedResponseMapper pagedResponseMapper, UserService userService, TaskRepository taskRepository,
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
        this.taskRepository = taskRepository;
    }

    @BusinessActivityLog(action = "Create Project")
    @Transactional
    public ProjectResponseDTO createProject(ProjectRequestRegisterDTO requestDTO, User actingUser) {
        projectTitleUniquenessValidator.validate(requestDTO.getTitle(), actingUser);

        Project newProject = new Project(requestDTO.getTitle(), requestDTO.getDescription(), actingUser);

        ProjectMember creatorMembership = new ProjectMember(actingUser, newProject, EProjectRole.OWNER);
        creatorMembership.acceptedInvitation();
        newProject.addMember(creatorMembership);

        Project savedProject = projectRepository.save(newProject);
        return modelMapper.map(savedProject, ProjectResponseDTO.class);
    }


    @BusinessActivityLog(action = "Update Project")
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


    @BusinessActivityLog(action = "Update Project Member Role")
    @Transactional
    public ProjectMemberResponseDTO updateMemberRole(Long projectId, Long userIdToUpdate, UpdateMemberRoleProjectRequestDTO dto, User actingUser) {
        Project project = findProjectByIdOrThrow(projectId);
        projectAuthorizationService.checkProjectAdmin(actingUser, project);

        User userToUpdate = userService.findUserEntityById(userIdToUpdate);
        ProjectMember membershipToUpdate = findMembershipByIdOrThrow(userToUpdate, project);
        ProjectMember actingMembership = findMembershipByIdOrThrow(actingUser, project);

        projectMembershipActionValidator.validateRoleUpdate(actingMembership, membershipToUpdate, dto.getNewRole());

        membershipToUpdate.changeRole(dto.getNewRole());
        ProjectMember updatedMembership = projectMemberRepository.save(membershipToUpdate);

        return toProjectMemberResponseDTO(updatedMembership);
    }

    @BusinessActivityLog(action = "Import Team Members to Project")
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

    public Project getProjectByIdForReport(Long projectId, Long userId, User actingUser) {
        Project project = findProjectByIdForReportOrThrow(projectId, userId);
        projectAuthorizationService.checkProjectAdmin(actingUser, project);
        return project;
    }

    @BusinessActivityLog(action = "Generate Project Report")
    public ProjectReportDTO getProjectReport(Long projectId, User actingUser) {
        projectAuthorizationService.checkProjectMembership(actingUser, findProjectByIdOrThrow(projectId));

        Project projectWithTasks = findProjectWithMembersAndTasksOrThrow(projectId);
        TaskSummaryDTO summary = calculateTaskSummary(projectWithTasks.getTasks());

        List<MemberPerformanceDTO> ranking = calculateMemberPerformanceRanking(projectId);

        return ProjectReportDTO.builder()
                .summary(summary)
                .memberPerformanceRanking(ranking)
                .build();
    }

    @BusinessActivityLog(action = "Invite Member to Project")
    @Transactional
    public void inviteMemberByEmail(Long projectId, InviteProjectMemberRequestDTO requestDTO, User actingUser) {
        Project project = findProjectByIdOrThrow(projectId);
        projectAuthorizationService.checkProjectAdmin(actingUser, project);
        projectInvitationValidator.validateRole(requestDTO.getRole());

        User userToInvite = userService.findUserEntityByEmail(requestDTO.getEmail());

        projectMembershipValidator.validateNewMember(userToInvite, project);

        ProjectMember newMember = new ProjectMember(userToInvite, project, requestDTO.getRole());
        String token = newMember.generateInvitation();

        project.addMember(newMember);
        projectRepository.save(project);

        emailService.sendProjectInvitationEmail(userToInvite.getEmail(), project.getTitle(), token);
    }

    @BusinessActivityLog(action = "Accept Project Invitation via Email Token")
    @Transactional
    public void acceptInvitationFromEmail(String token) {
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("Invitation token cannot be null or empty.");
        }

        ProjectMember membership = findByInvitationTokenEmailOrThrow(token);
        projectTokenValidator.validateInvitationEmailToken(membership);

        membership.acceptedInvitation();

        projectMemberRepository.save(membership);
    }

    @BusinessActivityLog(action = "Generate Project Invitation Link")
    @Transactional
    public InviteLinkResponseDTO generateInvitedLink(Long projectId, User actingUser) {
        Project project = findProjectByIdOrThrow(projectId);
        projectAuthorizationService.checkProjectAdmin(actingUser, project);

        String token = project.generateInviteLinkToken();

        projectRepository.save(project);

        return new InviteLinkResponseDTO(baseUrl + "/api/project/join?token=" + token,
            project.getInvitationTokenExpiry());
    }

    @BusinessActivityLog(action = "Accept Project Invitation via Link")
    @Transactional
    public ProjectMemberResponseDTO acceptInvitationFromLink(String token, User actingUser) {
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("Invitation token cannot be null or empty.");
        }

        Project project = findByInvitationTokenLinkOrThrow(token);

        projectTokenValidator.validateInvitationLinkToken(project);
        projectMembershipValidator.validateNewMember(actingUser, project);

        ProjectMember newMember = new ProjectMember(actingUser, project, EProjectRole.MEMBER);
        newMember.acceptedInvitation();

        project.addMember(newMember);
        projectRepository.save(project);

        return toProjectMemberResponseDTO(newMember);
    }

    @BusinessActivityLog(action = "Delete Project")
    @Transactional
    public void deleteProject(Long projectId, User actingUser) {
        Project project = findProjectByIdOrThrow(projectId);
        projectAuthorizationService.checkProjectOwner(actingUser, project);
        projectRepository.delete(project);
    }

    @BusinessActivityLog(action = "Remove Member from Project")
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

    public Set<User> findProjectMembersFromIdList(Long projectId, List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new HashSet<>();
        }
        return projectMemberRepository.findProjectMembersAsUsers(projectId, userIds);
    }

    public TaskSummaryDTO calculateTaskSummary(Set<Task> tasks) {
        long doneCount = 0;
        long inProgressCount = 0;
        long toDoCount = 0;
        long overdueCount = 0;

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
        return new TaskSummaryDTO(totalCount, doneCount, inProgressCount, toDoCount, overdueCount);
    }

    public Set<Task> findFilteredTasksForProject(Long projectId, TaskFilterReportDTO filter) {
        return taskRepository.findTasksByProjectWithFiltersForReport(projectId, filter);
    }

    private List<MemberPerformanceDTO> calculateMemberPerformanceRanking(Long projectId) {
        List<Task> completedTasks = taskRepository.findAllByProjectIdAndStatusWithAssignments(projectId, ETaskStatus.DONE);
        return completedTasks.stream()
                .flatMap(task -> task.getAssignments().stream())
                .collect(Collectors.groupingBy(
                        assignment -> assignment.getUser(),
                        Collectors.counting()
                ))
                .entrySet().stream()
                .map(entry -> new MemberPerformanceDTO(
                        entry.getKey().getId(),
                        entry.getKey().getUsername(),
                        entry.getValue()
                ))
                .sorted(Comparator.comparingLong(MemberPerformanceDTO::getCompletedTasksCount).reversed())
                .collect(Collectors.toList());
    }

    public Project findProjectEntityById(Long teamId) {
        return findProjectByIdOrThrow(teamId);
    }

    private Project findProjectByIdForReportOrThrow(Long projectId, Long userId) {
        return projectRepository.findProjectByIdForReport(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id and user"));
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


    private Project findProjectWithMembersAndTasksOrThrow(Long projectId) {
        return projectRepository.findByIdWithMembersAndTasks(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));
    }


}


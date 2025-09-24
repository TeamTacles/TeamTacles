package br.com.teamtacles.task.service;

import br.com.teamtacles.common.dto.response.page.PagedResponse;
import br.com.teamtacles.common.exception.ResourceNotFoundException;
import br.com.teamtacles.common.mapper.PagedResponseMapper;
import br.com.teamtacles.config.aop.BusinessActivityLog;
import br.com.teamtacles.project.model.Project;
import br.com.teamtacles.project.service.ProjectService;
import br.com.teamtacles.project.service.ProjectAuthorizationService;
import br.com.teamtacles.task.dto.request.*;
import br.com.teamtacles.task.dto.response.TaskResponseDTO;
import br.com.teamtacles.task.dto.response.UserAssignmentResponseDTO;
import br.com.teamtacles.task.enumeration.ETaskRole;
import br.com.teamtacles.task.enumeration.ETaskStatus;
import br.com.teamtacles.task.model.Task;
import br.com.teamtacles.task.model.TaskAssignment;
import br.com.teamtacles.task.repository.TaskAssignmentRepository;
import br.com.teamtacles.task.repository.TaskRepository;
import br.com.teamtacles.task.validator.TaskAssignmentRoleValidator;
import br.com.teamtacles.task.validator.TaskProjectAssociationValidator;
import br.com.teamtacles.task.validator.TaskStateTransitionValidator;
import br.com.teamtacles.user.model.User;
import br.com.teamtacles.user.service.UserService;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final TaskProjectAssociationValidator taskProjectAssociationValidator;
    private final TaskStateTransitionValidator taskStateTransitionValidator;
    private final TaskAssignmentRoleValidator taskAssignmentRoleValidator;
    private final ProjectService projectService;
    private final UserService userService;
    private final ProjectAuthorizationService projectAuthorizationService;
    private final TaskAuthorizationService taskAuthorizationService;
    private final ModelMapper modelMapper;
    private final PagedResponseMapper pagedResponseMapper;

    public TaskService(TaskRepository taskRepository,
                       TaskAssignmentRepository taskAssignmentRepository,
                       TaskProjectAssociationValidator taskProjectAssociationValidator,
                       TaskStateTransitionValidator taskStateTransitionValidator,
                       TaskAssignmentRoleValidator taskAssignmentRoleValidator,
                       ProjectService projectService,
                       UserService userService,
                       ProjectAuthorizationService projectAuthorizationService,
                       TaskAuthorizationService taskAuthorizationService,
                       ModelMapper modelMapper,
                       PagedResponseMapper pagedResponseMapper) {
        this.taskRepository = taskRepository;
        this.taskAssignmentRepository = taskAssignmentRepository;
        this.taskProjectAssociationValidator = taskProjectAssociationValidator;
        this.taskStateTransitionValidator = taskStateTransitionValidator;
        this.taskAssignmentRoleValidator = taskAssignmentRoleValidator;
        this.projectService = projectService;
        this.userService = userService;
        this.projectAuthorizationService = projectAuthorizationService;
        this.taskAuthorizationService = taskAuthorizationService;
        this.modelMapper = modelMapper;
        this.pagedResponseMapper = pagedResponseMapper;
    }

    @BusinessActivityLog(action = "Create Task")
    @Transactional
    public TaskResponseDTO createTask(Long projectId, TaskRequestRegisterDTO taskDto, User actingUser) {
        Project project = projectService.findProjectEntityById(projectId);
        projectAuthorizationService.checkProjectMembership(actingUser, project);

        Task task = modelMapper.map(taskDto, Task.class);
        task.setProject(project);
        task.setOwner(actingUser);

        TaskAssignment ownerAssignment = new TaskAssignment(task, actingUser, ETaskRole.OWNER);
        task.addAssigment(ownerAssignment);

        Task savedTask = taskRepository.save(task);
        return modelMapper.map(savedTask, TaskResponseDTO.class);
    }

    @BusinessActivityLog(action = "Update Task Status")
    @Transactional
    public TaskResponseDTO updateTaskStatus(Long projectId, Long taskId, UpdateTaskStatusRequestDTO updateStatusDTO, User actingUser) {
        Task task = taskProjectAssociationValidator.findAndValidate(taskId, projectId);
        taskAuthorizationService.checkChangeStatusPermission(actingUser, task);

        taskStateTransitionValidator.validate(task.getStatus(), updateStatusDTO.getNewStatus());

        task.setStatus(updateStatusDTO.getNewStatus());

        if (updateStatusDTO.getNewStatus() == ETaskStatus.DONE) {
            task.setCompletedAt(OffsetDateTime.now());
            task.setCompletionComment(updateStatusDTO.getCompletionComment());
        }

        Task updateTask = taskRepository.save(task);
        return modelMapper.map(updateTask, TaskResponseDTO.class);
    }

    public PagedResponse<TaskResponseDTO> getTasksForProject(Pageable pageable, Long projectId, TaskFilterReportDTO filter, User actingUser) {
        Project project = projectService.findProjectEntityById(projectId);
        projectAuthorizationService.checkProjectMembership(actingUser, project);

        Page<Task> tasks = taskRepository.findTasksByProjectWithFilters(projectId, filter, pageable);
        Page<TaskResponseDTO> taskResponseDTOPage = tasks.map(task -> modelMapper.map(task, TaskResponseDTO.class));

        return pagedResponseMapper.toPagedResponse(taskResponseDTOPage, TaskResponseDTO.class);
    }

    @BusinessActivityLog(action = "Assign Users to Task")
    @Transactional
    public TaskResponseDTO assignUsersToTask(Long projectId, Long taskId, Set<TaskAssignmentRequestDTO> assignmentsDTO, User actingUser) {
        Task task = taskProjectAssociationValidator.findAndValidate(taskId, projectId);
        taskAuthorizationService.checkEditPermission(actingUser, task);

        taskAssignmentRoleValidator.validate(assignmentsDTO);

        List<Long> userIdsToAssign = assignmentsDTO.stream()
                .map(TaskAssignmentRequestDTO::getUserId).toList();

        Set<User> validMembers = projectService.findProjectMembersFromIdList(projectId, userIdsToAssign);

        if (userIdsToAssign.size() != validMembers.size()) {
            throw new AccessDeniedException("One or more users are not valid members of this project.");
        }

        Set<User> alreadyAssignedUsers = task.getAssignments().stream()
                .map(TaskAssignment::getUser)
                .collect(Collectors.toSet());

        for (TaskAssignmentRequestDTO assignmentDTO : assignmentsDTO) {
            User userToAssign = validMembers.stream()
                    .filter(a -> a.getId().equals(assignmentDTO.getUserId()))
                    .findFirst().orElseThrow();

            if (!alreadyAssignedUsers.contains(userToAssign)) {
                TaskAssignment newAssignment = new TaskAssignment(task, userToAssign, assignmentDTO.getTaskRole());
                task.addAssigment(newAssignment);
            }
        }

        Task updatedTask = taskRepository.save(task);
        return modelMapper.map(updatedTask, TaskResponseDTO.class);
    }

    @BusinessActivityLog(action = "Remove Users from Task")
    @Transactional
    public void removeUsersFromTask(Long projectId, Long taskId, Set<Long> userIdsToRemove, User actingUser) {
        Task task = taskProjectAssociationValidator.findAndValidate(taskId, projectId);
        taskAuthorizationService.checkEditPermission(actingUser, task);

        if (userIdsToRemove.contains(task.getOwner().getId())) {
            throw new IllegalArgumentException("The task owner cannot be removed");
        }

        taskAssignmentRepository.deleteAllByTaskIdAndUserIds(taskId, userIdsToRemove);
    }
    @BusinessActivityLog(action = "Delete Task")

    @Transactional
    public void deleteTaskById(Long projectId, Long taskId, User actingUser) {
        Task task = taskProjectAssociationValidator.findAndValidate(taskId, projectId);
        taskAuthorizationService.checkEditPermission(actingUser, task);
        taskRepository.delete(task);
    }

    @BusinessActivityLog(action = "Update Task Details")
    @Transactional
    public TaskResponseDTO updateTaskDetails(Long projectId, Long taskId, TaskRequestUpdateDTO taskUpdateDTO, User actingUser) {
        Task task = taskProjectAssociationValidator.findAndValidate(taskId, projectId);

        taskAuthorizationService.checkEditPermission(actingUser, task);

        if (taskUpdateDTO.getTitle() != null) {
            task.setTitle(taskUpdateDTO.getTitle());
        }
        if (taskUpdateDTO.getDescription() != null) {
            task.setDescription(taskUpdateDTO.getDescription());
        }
        if (taskUpdateDTO.getDueDate() != null) {
            task.setDueDate(taskUpdateDTO.getDueDate());
        }

        Task updatedTask = taskRepository.save(task);
        return modelMapper.map(updatedTask, TaskResponseDTO.class);

    }

    private TaskAssignment findByTaskAndUserOrThrow(Task task, User userToRemove) {
        return taskAssignmentRepository.findByTaskAndUser(task, userToRemove)
                .orElseThrow((() -> new ResourceNotFoundException("User to remove is not assigned to this task.")));
    }

    @Transactional
    public TaskResponseDTO getTaskById(Long projectId, Long taskId, User actingUser) {
        Task task = taskProjectAssociationValidator.findAndValidate(taskId, projectId);

        taskAuthorizationService.checkViewPermission(actingUser, task);

        return modelMapper.map(task, TaskResponseDTO.class);
    }

    public Task findTaskByIdOrThrow(Long taskId) {
        String errorMessage = String.format("Task with id '%d' not found.", taskId);
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException(errorMessage));
    }

    @Transactional(readOnly = true)
    public List<UserAssignmentResponseDTO> getTaskMembers(Long projectId, Long taskId, User actingUser) {
        Task task = taskProjectAssociationValidator.findAndValidate(taskId, projectId);
        taskAuthorizationService.checkViewPermission(actingUser, task);

        List<TaskAssignment> assignments = taskAssignmentRepository.findAllByTaskId(taskId);

        return assignments.stream()
                .map(this::toUserAssignmentResponseDTO)
                .collect(Collectors.toList());
    }



    private UserAssignmentResponseDTO toUserAssignmentResponseDTO(TaskAssignment assignment) {
        UserAssignmentResponseDTO dto = new UserAssignmentResponseDTO();
        dto.setUserId(assignment.getUser().getId());
        dto.setUsername(assignment.getUser().getUsername());
        dto.setTaskRole(assignment.getTaskRole());
        return dto;
    }


}
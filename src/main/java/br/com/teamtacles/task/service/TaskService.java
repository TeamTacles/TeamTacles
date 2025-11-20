package br.com.teamtacles.task.service;

import br.com.teamtacles.common.dto.response.page.PagedResponse;
import br.com.teamtacles.common.exception.ResourceNotFoundException;
import br.com.teamtacles.common.mapper.PagedResponseMapper;
import br.com.teamtacles.config.aop.BusinessActivityLog;
import br.com.teamtacles.project.dto.response.ProjectResponseDTO;
import br.com.teamtacles.project.model.Project;
import br.com.teamtacles.project.service.ProjectService;
import br.com.teamtacles.project.service.ProjectAuthorizationService;
import br.com.teamtacles.task.dto.request.*;
import br.com.teamtacles.task.dto.response.TaskResponseDTO;
import br.com.teamtacles.task.dto.response.TaskUpdateStatusResponseDTO;
import br.com.teamtacles.task.dto.response.UserAssignmentResponseDTO;
import br.com.teamtacles.task.dto.response.UserTaskResponseDTO;
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
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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

        Task task = new Task(project, taskDto.getTitle(), taskDto.getDescription(), actingUser, taskDto.getDueDate());
        TaskAssignment ownerAssignment = new TaskAssignment(task, actingUser, ETaskRole.OWNER);
        task.addAssigment(ownerAssignment);

        Task savedTask = taskRepository.save(task);
        return modelMapper.map(savedTask, TaskResponseDTO.class);
    }

    @BusinessActivityLog(action = "Update Task Status")
    @Transactional
    public TaskUpdateStatusResponseDTO updateTaskStatus(Long projectId, Long taskId, UpdateTaskStatusRequestDTO updateStatusDTO, User actingUser) {
        Task task = taskProjectAssociationValidator.findAndValidate(taskId, projectId);
        taskAuthorizationService.checkChangeStatusPermission(actingUser, task);

        taskStateTransitionValidator.validate(task.getStatus(), updateStatusDTO.getNewStatus());

        if(updateStatusDTO.getNewStatus() == ETaskStatus.DONE){
            task.completedTask(updateStatusDTO.getCompletionComment());
        } else {
            task.updateStatus(updateStatusDTO.getNewStatus());
        }

        Task updateTask = taskRepository.save(task);
        return modelMapper.map(updateTask, TaskUpdateStatusResponseDTO.class);
    }

    public PagedResponse<TaskResponseDTO> getTasksForProject(Pageable pageable, Long projectId, TaskFilterReportDTO filter, User actingUser) {
        Project project = projectService.findProjectEntityById(projectId);
        projectAuthorizationService.checkProjectMembership(actingUser, project);

        Page<Task> tasks = taskRepository.findTasksByProjectWithFilters(projectId, filter, pageable);
        Page<TaskResponseDTO> taskResponseDTOPage = tasks.map(task -> modelMapper.map(task, TaskResponseDTO.class));

        return pagedResponseMapper.toPagedResponse(taskResponseDTOPage, TaskResponseDTO.class);
    }

    public TaskResponseDTO getTaskById(Long projectId, Long taskId, User actingUser) {
        Task task = taskProjectAssociationValidator.findAndValidate(taskId, projectId);

        taskAuthorizationService.checkViewPermission(actingUser, task);

        return modelMapper.map(task, TaskResponseDTO.class);
    }

    public List<UserAssignmentResponseDTO> getTaskMembers(Long projectId, Long taskId, User actingUser) {
        Task task = taskProjectAssociationValidator.findAndValidate(taskId, projectId);
        taskAuthorizationService.checkViewPermission(actingUser, task);

        List<TaskAssignment> assignments = taskAssignmentRepository.findAllByTaskId(taskId);

        return assignments.stream()
                .map(this::toUserAssignmentResponseDTO)
                .collect(Collectors.toList());
    }

    public PagedResponse<UserTaskResponseDTO> getAllTasksByUser(Pageable pageable, TaskFilterReportDTO filter, User actingUser) {
        Page<Task> tasksPage = taskRepository.findTasksByUserWithFilters(actingUser.getId(), filter, pageable);

        Page<UserTaskResponseDTO> userTaskDTOPage = tasksPage.map(this::toUserTaskResponseDTO);

        return pagedResponseMapper.toPagedResponse(userTaskDTOPage, UserTaskResponseDTO.class);
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
                    .findFirst().orElseThrow(() -> new ResourceNotFoundException("One or more users are not valid members of this project."));

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

        Set<TaskAssignment> assignmentsToRemove = taskAssignmentRepository.findAllByTaskIdAndUserIds(taskId, userIdsToRemove);

        for(TaskAssignment assignmentToRemove: assignmentsToRemove) {
            task.removeAssigment(assignmentToRemove);
        }

        taskRepository.save(task);
    }

    @BusinessActivityLog(action = "Delete Task")
    @Transactional
    public void deleteTaskById(Long projectId, Long taskId, User actingUser) {
        Task task = taskProjectAssociationValidator.findAndValidate(taskId, projectId);
        taskAuthorizationService.checkEditPermission(actingUser, task);
        taskRepository.delete(task);
    }

    @Transactional
    public void leaveTask(Long taskId, User actingUser) {
        Task task = findTaskByIdOrThrow(taskId);
        taskAuthorizationService.checkChangeStatusPermission(actingUser, task);

        boolean isOwner = task.getOwner().equals(actingUser);

        if(isOwner) {
            List<TaskAssignment> members = task.getAssignments().stream()
                    .filter(m -> !m.getUser().equals(actingUser))
                    .toList();

            if(members.isEmpty()) {
                taskRepository.delete(task);
            } else {
                transferTaskOwnership(members, task);
                removeAssignmentForUser(task, actingUser);
            }
        } else {
            removeAssignmentForUser(task, actingUser);
        }
    }

    // Sobrecarga temporária - Refatorar depois
    private void leaveTask(Task task, User actingUser) {
        boolean isOwner = task.getOwner().equals(actingUser);

        if(isOwner) {
            List<TaskAssignment> members = task.getAssignments().stream()
                    .filter(m -> !m.getUser().equals(actingUser))
                    .toList();

            if(members.isEmpty()) {
                taskRepository.delete(task);
            } else {
                transferTaskOwnership(members, task);
                removeAssignmentForUser(task, actingUser);
            }
        } else {
            removeAssignmentForUser(task, actingUser);
        }
    }

    @Transactional
    public void leaveAllTasks(Long projectId, User actingUser) {

        Set<TaskAssignment> assignments = taskAssignmentRepository.findAllByProjectAndUser(projectId, actingUser);

        for(TaskAssignment assignment : assignments) {
           Task task = assignment.getTask();
           leaveTask(task, actingUser);
        }
    }

    @Transactional
    public void handleOwnerDeletion(User user) {
        List<Task> tasks = taskRepository.findAllByOwner(user);

        for(Task task : tasks) {
            List<TaskAssignment> members = task.getAssignments().stream()
                    .filter(m -> !m.getUser().equals(user))
                    .toList();

            if(members.isEmpty()) {
                taskRepository.delete(task);
            } else {
                transferTaskOwnership(members, task);
                removeAssignmentForUser(task, user);
            }
        }
    }

    private void transferTaskOwnership(List<TaskAssignment> members, Task task) {
        Optional<TaskAssignment> newOwnerMember = members.stream()
                .min(Comparator.comparing(TaskAssignment::getAssignedAt));

        newOwnerMember.ifPresent(member -> {
            User newOwner = member.getUser();
            task.transferOwnership(newOwner);
            member.changeRole(ETaskRole.OWNER);
            taskAssignmentRepository.save(member);
            taskRepository.save(task);
        });
    }

    private void removeAssignmentForUser(Task task, User user) {
        TaskAssignment member = task.getAssignments().stream()
                .filter(m -> m.getUser().equals(user))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("User to update not found in this task."));

        task.removeAssigment(member);
        taskRepository.save(task);
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

    public Task findTaskByIdOrThrow(Long taskId) {
        String errorMessage = String.format("Task with id '%d' not found.", taskId);
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException(errorMessage));
    }

    private UserAssignmentResponseDTO toUserAssignmentResponseDTO(TaskAssignment assignment) {
        UserAssignmentResponseDTO dto = new UserAssignmentResponseDTO();
        dto.setUserId(assignment.getUser().getId());
        dto.setUsername(assignment.getUser().getUsername());
        dto.setTaskRole(assignment.getTaskRole());
        return dto;
    }

    private UserTaskResponseDTO toUserTaskResponseDTO(Task task) {
        UserTaskResponseDTO taskDto = new UserTaskResponseDTO();
        ProjectResponseDTO projectDto = new ProjectResponseDTO();

        projectDto.setId(task.getProject().getId());
        projectDto.setTitle(task.getProject().getTitle());

        taskDto.setId(task.getId());
        taskDto.setTitle(task.getTitle());
        taskDto.setDescription(task.getDescription());
        taskDto.setTaskStatus(task.getEffectiveStatus());
        taskDto.setDueDate(task.getDueDate());
        taskDto.setProject(projectDto);
        return taskDto;
    }
}
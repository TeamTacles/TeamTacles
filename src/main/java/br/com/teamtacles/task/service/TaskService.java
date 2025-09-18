package br.com.teamtacles.task.service;

import br.com.teamtacles.common.dto.response.page.PagedResponse;
import br.com.teamtacles.common.exception.ResourceNotFoundException;
import br.com.teamtacles.common.mapper.PagedResponseMapper;
import br.com.teamtacles.project.model.Project;
import br.com.teamtacles.project.service.ProjectService;
import br.com.teamtacles.project.service.ProjectAuthorizationService;
import br.com.teamtacles.task.dto.request.TaskAssignmentRequestDTO;
import br.com.teamtacles.task.dto.request.TaskRequestRegisterDTO;
import br.com.teamtacles.task.dto.response.TaskResponseDTO;
import br.com.teamtacles.task.model.Task;
import br.com.teamtacles.task.model.TaskAssignment;
import br.com.teamtacles.task.repository.TaskAssignmentRepository;
import br.com.teamtacles.task.repository.TaskRepository;
import br.com.teamtacles.task.validator.TaskProjectAssociationValidator;
import br.com.teamtacles.user.model.User;
import br.com.teamtacles.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final TaskProjectAssociationValidator taskProjectAssociationValidator;
    private final ProjectService projectService;
    private final UserService userService;
    private final ProjectAuthorizationService projectAuthorizationService;
    private final TaskAuthorizationService taskAuthorizationService;
    private final ModelMapper modelMapper;
    private final PagedResponseMapper pagedResponseMapper;

    public TaskService (TaskRepository taskRepository,TaskAssignmentRepository taskAssignmentRepository,
                        TaskProjectAssociationValidator taskProjectAssociationValidator,
                        ProjectService projectService, ProjectAuthorizationService projectAuthorizationService,
                        TaskAuthorizationService taskAuthorizationService, ModelMapper modelMapper,
                        PagedResponseMapper pagedResponseMapper, UserService userService) {
        this.taskRepository = taskRepository;
        this.taskAssignmentRepository = taskAssignmentRepository;
        this.taskProjectAssociationValidator = taskProjectAssociationValidator;
        this.projectService = projectService;
        this.projectAuthorizationService = projectAuthorizationService;
        this.taskAuthorizationService = taskAuthorizationService;
        this.modelMapper = modelMapper;
        this.pagedResponseMapper = pagedResponseMapper;
        this.userService = userService;
    }

    @Transactional
    public TaskResponseDTO createTask(Long projectId, TaskRequestRegisterDTO taskDto, User actingUser) {
        Project project = projectService.findProjectEntityById(projectId);
        projectAuthorizationService.checkProjectMembership(actingUser, project);

        Task task = modelMapper.map(taskDto, Task.class);
        task.setProject(project);
        task.setOwner(actingUser);

        Task savedTask = taskRepository.save(task);
        return modelMapper.map(savedTask, TaskResponseDTO.class);
    }

    public PagedResponse<TaskResponseDTO> getTasksForProject(Pageable pageable, Long projectId, User actingUser) {
        Project project = projectService.findProjectEntityById(projectId);
        projectAuthorizationService.checkProjectMembership(actingUser, project);

        Page<Task> tasks = taskRepository.findByProject(pageable, project);
        Page<TaskResponseDTO> taskResponseDTOPage = tasks.map(task -> modelMapper.map(task, TaskResponseDTO.class));

        return pagedResponseMapper.toPagedResponse(taskResponseDTOPage, TaskResponseDTO.class);
    }

    public TaskResponseDTO assignUsersToTask(Long projectId, Long taskId, List<TaskAssignmentRequestDTO> assignmentsDTO, User actingUser) {
        Task task = taskProjectAssociationValidator.findAndValidte(taskId, projectId);
        taskAuthorizationService.checkEditPermission(actingUser, task);

        for (TaskAssignmentRequestDTO assignmentDTO : assignmentsDTO) {
            User userToAssign = userService.findUserEntityById(assignmentDTO.getUserId());
            projectAuthorizationService.checkProjectMembership(userToAssign, task.getProject());

            boolean alreadyAssigned = task.getAssignments().stream().anyMatch(a -> a.getUser().equals(userToAssign));

            if(!alreadyAssigned) {
                TaskAssignment newAssignment = new TaskAssignment(task, userToAssign, assignmentDTO.getRole());
                task.addAssigment(newAssignment);
            }
        }

        Task updatedTask = taskRepository.save(task);
        return modelMapper.map(updatedTask, TaskResponseDTO.class);
    }

    public void removeUserFromTask(Long projectId, Long taskId, Long userIdToRemove, User actingUser) {
        Task task = taskProjectAssociationValidator.findAndValidte(taskId, projectId);
        taskAuthorizationService.checkEditPermission(actingUser, task);

        User userToRemove = userService.findUserEntityById(userIdToRemove);
        TaskAssignment assignmentToRemove = findByTaskAndUserOrThroe(task, userToRemove);

        taskAssignmentRepository.delete(assignmentToRemove);
    }

    public void deleteTaskById(Long projectId, Long taskId, User actingUser) {
        Task task = taskProjectAssociationValidator.findAndValidte(taskId, projectId);
        taskAuthorizationService.checkEditPermission(actingUser, task);
        taskRepository.delete(task);
    }

    private TaskAssignment findByTaskAndUserOrThroe(Task task, User userToRemove ) {
        return taskAssignmentRepository.findByTaskAndUser(task, userToRemove)
                .orElseThrow((() -> new ResourceNotFoundException("User to remove is not assigned to this task.")));
    }
}
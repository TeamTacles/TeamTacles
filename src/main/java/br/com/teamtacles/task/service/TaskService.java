package br.com.teamtacles.task.service;

import br.com.teamtacles.common.dto.response.page.PagedResponse;
import br.com.teamtacles.common.mapper.PagedResponseMapper;
import br.com.teamtacles.project.model.Project;
import br.com.teamtacles.project.service.ProjectService;
import br.com.teamtacles.project.service.ProjectAuthorizationService;
import br.com.teamtacles.task.dto.request.TaskRequestRegisterDTO;
import br.com.teamtacles.task.dto.response.TaskResponseDTO;
import br.com.teamtacles.task.model.Task;
import br.com.teamtacles.task.repository.TaskRepository;
import br.com.teamtacles.user.model.User;
import br.com.teamtacles.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectService projectService;
    private final ProjectAuthorizationService projectAuthService;
    private final ModelMapper modelMapper;
    private final PagedResponseMapper pagedResponseMapper;

    public TaskService (TaskRepository taskRepository, ProjectService projectService, ProjectAuthorizationService projectAuthService, ModelMapper modelMapper, PagedResponseMapper pagedResponseMapper) {
        this.taskRepository = taskRepository;
        this.projectService = projectService;
        this.projectAuthService = projectAuthService;
        this.modelMapper = modelMapper;
        this.pagedResponseMapper = pagedResponseMapper;
    }

    @Transactional
    public TaskResponseDTO createTask(Long projectId, TaskRequestRegisterDTO taskDto, User actingUser) {
        Project project = projectService.findProjectEntityById(projectId);
        projectAuthService.checkProjectMembership(actingUser, project);

        Task task = modelMapper.map(taskDto, Task.class);
        task.setProject(project);
        task.setOwner(actingUser);

        Task savedTask = taskRepository.save(task);
        return modelMapper.map(savedTask, TaskResponseDTO.class);
    }

    public PagedResponse<TaskResponseDTO> getTasksForProject(Pageable pageable, Long projectId, User actingUser) {
        Project project = projectService.findProjectEntityById(projectId);
        projectAuthService.checkProjectMembership(actingUser, project);

        Page<Task> tasks = taskRepository.findByProject(pageable, project);
        Page<TaskResponseDTO> taskResponseDTOPage = tasks.map(task -> modelMapper.map(task, TaskResponseDTO.class));

        return pagedResponseMapper.toPagedResponse(taskResponseDTOPage, TaskResponseDTO.class);
    }
}
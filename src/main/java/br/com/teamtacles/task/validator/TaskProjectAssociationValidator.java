package br.com.teamtacles.task.validator;

import br.com.teamtacles.common.exception.ResourceNotFoundException;
import br.com.teamtacles.task.model.Task;
import br.com.teamtacles.task.repository.TaskRepository;
import org.springframework.stereotype.Component;

@Component
public class TaskProjectAssociationValidator {

    private final TaskRepository taskRepository;

    public TaskProjectAssociationValidator(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public Task findAndValidate(Long taskId, Long projectId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        if(!task.getProject().getId().equals(projectId)) {
            throw new ResourceNotFoundException("Task not found with id: " + taskId);
        }

        return task;
    }
}

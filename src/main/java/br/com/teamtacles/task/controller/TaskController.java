package br.com.teamtacles.task.controller;

import br.com.teamtacles.common.dto.response.page.PagedResponse;
import br.com.teamtacles.security.UserAuthenticated;
import br.com.teamtacles.task.dto.request.TaskAssignmentRequestDTO;
import br.com.teamtacles.task.dto.request.TaskRequestRegisterDTO;
import br.com.teamtacles.task.dto.response.TaskResponseDTO;
import br.com.teamtacles.task.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/project/{projectId}/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<TaskResponseDTO> createTask(
            @PathVariable Long projectId,
            @RequestBody @Valid TaskRequestRegisterDTO taskDto,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        TaskResponseDTO createdTask = taskService.createTask(projectId, taskDto, authenticatedUser.getUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTask);
    }

    @PostMapping("/{taskId}/assignments")
    public ResponseEntity<TaskResponseDTO> assignUsers(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @RequestBody @Valid List<TaskAssignmentRequestDTO> assignmentsDTO,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        TaskResponseDTO updatedTask = taskService.assignUsersToTask(projectId, taskId, assignmentsDTO, authenticatedUser.getUser());
        return ResponseEntity.ok(updatedTask);
    }

    @GetMapping
    public ResponseEntity<PagedResponse<TaskResponseDTO>> getTasksForProject(
            @PathVariable Long projectId,
            Pageable pageable,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        PagedResponse<TaskResponseDTO> tasks = taskService.getTasksForProject(pageable, projectId, authenticatedUser.getUser());
        return ResponseEntity.ok(tasks);
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTaskById(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        taskService.deleteTaskById(projectId, taskId, authenticatedUser.getUser());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{taskId}/assignments/{userId}")
    public ResponseEntity<Void> removeUserFromTask(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @PathVariable Long userId,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        taskService.removeUserFromTask(projectId, taskId, userId, authenticatedUser.getUser());
        return ResponseEntity.noContent().build();
    }
}
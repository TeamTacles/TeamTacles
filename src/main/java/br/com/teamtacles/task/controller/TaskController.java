package br.com.teamtacles.task.controller;

import br.com.teamtacles.common.dto.response.page.PagedResponse;
import br.com.teamtacles.security.UserAuthenticated;
import br.com.teamtacles.task.dto.request.*;
import br.com.teamtacles.task.dto.response.TaskResponseDTO;
import br.com.teamtacles.task.dto.response.UserAssignmentResponseDTO;
import br.com.teamtacles.task.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import br.com.teamtacles.task.dto.request.TaskAssignmentsBulkDeleteRequestDTO;

import java.util.List;
import java.util.Set;

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
            @RequestBody @Valid Set<TaskAssignmentRequestDTO> assignmentsDTO,
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

    @PatchMapping("/{taskId}/status")
    public ResponseEntity<TaskResponseDTO> updateTaskstatus (
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @RequestBody @Valid UpdateTaskStatusRequestDTO updateStatusDTO,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        TaskResponseDTO taskUpdate = taskService.updateTaskStatus(projectId, taskId, updateStatusDTO, authenticatedUser.getUser());
        return ResponseEntity.ok(taskUpdate);
    }

    @PatchMapping("/{taskId}")
    public ResponseEntity<TaskResponseDTO> updateTaskDetails(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @RequestBody @Valid TaskRequestUpdateDTO taskUpdateDTO,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        TaskResponseDTO updatedTask = taskService.updateTaskDetails(projectId, taskId, taskUpdateDTO, authenticatedUser.getUser());
        return ResponseEntity.ok(updatedTask);
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTaskById(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        taskService.deleteTaskById(projectId, taskId, authenticatedUser.getUser());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<TaskResponseDTO> getTaskById(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        TaskResponseDTO taskResponse = taskService.getTaskById(projectId, taskId, authenticatedUser.getUser());
        return ResponseEntity.ok(taskResponse);
    }

    @DeleteMapping("/{taskId}/assignments")
    public ResponseEntity<Void> removeUsersFromTask(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @RequestBody @Valid TaskAssignmentsBulkDeleteRequestDTO deleteRequest,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        taskService.removeUsersFromTask(projectId, taskId, deleteRequest.getUserIds(), authenticatedUser.getUser());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{taskId}/members")
    public ResponseEntity<List<UserAssignmentResponseDTO>> getTaskMembers(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        List<UserAssignmentResponseDTO> members = taskService.getTaskMembers(projectId, taskId, authenticatedUser.getUser());
        return ResponseEntity.ok(members);
    }
}
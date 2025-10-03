package br.com.teamtacles.task.controller;

import br.com.teamtacles.common.dto.response.page.PagedResponse;
import br.com.teamtacles.common.exception.ErrorResponse;
import br.com.teamtacles.security.UserAuthenticated;
import br.com.teamtacles.task.dto.request.*;
import br.com.teamtacles.task.dto.response.TaskResponseDTO;
import br.com.teamtacles.task.dto.response.TaskUpdateStatusResponseDTO;
import br.com.teamtacles.task.dto.response.UserAssignmentResponseDTO;
import br.com.teamtacles.task.service.TaskService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import br.com.teamtacles.task.dto.request.TaskAssignmentsBulkDeleteRequestDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/project/{projectId}/tasks")
@Tag(name = "Task Management", description = "Endpoints for creating, managing, and interacting with tasks within a project.")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @Operation(summary = "Create a new task", description = "Creates a new task within a project and sets the authenticated user as the owner.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Task created successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = TaskResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied, user is not a member of the project",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<TaskResponseDTO> createTask(
            @PathVariable Long projectId,
            @RequestBody @Valid TaskRequestRegisterDTO taskDto,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        TaskResponseDTO createdTask = taskService.createTask(projectId, taskDto, authenticatedUser.getUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTask);
    }

    @Operation(summary = "Assign users to a task", description = "Assigns one or more users to a task with specific roles. Requires edit permission on the task.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Users assigned successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = TaskResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied to edit the task",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Task or project not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{taskId}/assignments")
    public ResponseEntity<TaskResponseDTO> assignUsers(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @RequestBody @Valid Set<TaskAssignmentRequestDTO> assignmentsDTO,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        TaskResponseDTO updatedTask = taskService.assignUsersToTask(projectId, taskId, assignmentsDTO, authenticatedUser.getUser());
        return ResponseEntity.ok(updatedTask);
    }

    @Operation(summary = "List tasks for a project", description = "Retrieves a paginated and filtered list of tasks for a specific project.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied, user is not a member of the project",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<PagedResponse<TaskResponseDTO>> getTasksForProject(
            @PathVariable Long projectId,
            Pageable pageable,
            @ModelAttribute TaskFilterReportDTO filter,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        PagedResponse<TaskResponseDTO> tasks = taskService.getTasksForProject(pageable, projectId, filter, authenticatedUser.getUser());
        return ResponseEntity.ok(tasks);
    }

    @Operation(summary = "List members of a task", description = "Retrieves the list of all users assigned to a specific task.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task members retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied, user is not a member of the project",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Task or project not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{taskId}/members")
    public ResponseEntity<List<UserAssignmentResponseDTO>> getTaskMembers(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        List<UserAssignmentResponseDTO> members = taskService.getTaskMembers(projectId, taskId, authenticatedUser.getUser());
        return ResponseEntity.ok(members);
    }

    @Operation(summary = "Get a task by ID", description = "Retrieves the full details of a specific task.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task data retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = TaskResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Access denied, user is not a member of the project",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Task or project not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{taskId}")
    public ResponseEntity<TaskResponseDTO> getTaskById(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        TaskResponseDTO taskResponse = taskService.getTaskById(projectId, taskId, authenticatedUser.getUser());
        return ResponseEntity.ok(taskResponse);
    }

    @Operation(summary = "Update a task's status", description = "Updates the status of a task (e.g., from TO_DO to IN_PROGRESS).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task status updated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = TaskResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid status transition",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied to modify the status",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Task or project not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{taskId}/status")
    public ResponseEntity<TaskUpdateStatusResponseDTO> updateTaskstatus (
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @RequestBody @Valid UpdateTaskStatusRequestDTO updateStatusDTO,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        TaskUpdateStatusResponseDTO taskUpdate = taskService.updateTaskStatus(projectId, taskId, updateStatusDTO, authenticatedUser.getUser());
        return ResponseEntity.ok(taskUpdate);
    }

    @Operation(summary = "Update task details", description = "Updates the title, description, or due date of a task.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task details updated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = TaskResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Access denied to edit the task",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Task or project not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{taskId}")
    public ResponseEntity<TaskResponseDTO> updateTaskDetails(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @RequestBody @Valid TaskRequestUpdateDTO taskUpdateDTO,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        TaskResponseDTO updatedTask = taskService.updateTaskDetails(projectId, taskId, taskUpdateDTO, authenticatedUser.getUser());
        return ResponseEntity.ok(updatedTask);
    }

    @Operation(summary = "Delete a task", description = "Permanently removes a task. Requires edit permission on the task.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Task deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied to delete the task",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Task or project not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTaskById(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        taskService.deleteTaskById(projectId, taskId, authenticatedUser.getUser());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Remove users from a task", description = "Removes the assignment of one or more users from a task.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Users removed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request (e.g., trying to remove the task owner)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied to edit the task",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Task or project not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{taskId}/assignments")
    public ResponseEntity<Void> removeUsersFromTask(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @RequestBody @Valid TaskAssignmentsBulkDeleteRequestDTO deleteRequest,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        taskService.removeUsersFromTask(projectId, taskId, deleteRequest.getUserIds(), authenticatedUser.getUser());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Leave a task", description = "Allows an authenticated user to leave a task they are a member of.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Successfully left the task"),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden, the user is the owner and cannot leave the task",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Task or membership not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{taskId}/leave")
    public ResponseEntity<Void> leaveTask(
            @PathVariable Long taskId,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        taskService.leaveTask(taskId, authenticatedUser.getUser());
        return ResponseEntity.noContent().build();
    }
}
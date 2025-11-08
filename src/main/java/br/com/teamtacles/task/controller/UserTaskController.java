package br.com.teamtacles.task.controller;

import br.com.teamtacles.common.dto.response.page.PagedResponse;
import br.com.teamtacles.common.exception.ErrorResponse;
import br.com.teamtacles.security.UserAuthenticated;
import br.com.teamtacles.task.dto.request.TaskFilterReportDTO;
import br.com.teamtacles.task.dto.response.UserTaskResponseDTO;
import br.com.teamtacles.task.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
@Tag(name = "User Tasks", description = "Endpoints related to tasks specific to the authenticated user.")
public class UserTaskController {

    private final TaskService taskService;

    public UserTaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @Operation(summary = "List tasks for the authenticated user", description = "Retrieves a paginated list of tasks associated with the authenticated user, with optional filtering.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User's tasks retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<PagedResponse<UserTaskResponseDTO>> getAllTasksByUser(
            @ModelAttribute TaskFilterReportDTO filter,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser,
            Pageable pageable) {
        PagedResponse<UserTaskResponseDTO> tasks = taskService.getAllTasksByUser(pageable, filter, authenticatedUser.getUser());
        return ResponseEntity.ok(tasks);
    }
}
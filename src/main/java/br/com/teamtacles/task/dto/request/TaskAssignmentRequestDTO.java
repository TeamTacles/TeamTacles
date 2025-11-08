package br.com.teamtacles.task.dto.request;

import br.com.teamtacles.task.enumeration.ETaskRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "TaskAssignmentRequest", description = "DTO to assign a user to a task with a specific role.")
public class TaskAssignmentRequestDTO {

    @Schema(description = "The ID of the user to be assigned to the task.", example = "101", required = true)
    @NotNull(message = "User ID cannot be null.")
    private Long userId;

    @Schema(description = "The role to be assigned to the user for this task.", example = "ASSIGNEE", required = true)
    @NotNull(message = "Task role cannot be null.")
    private ETaskRole taskRole;
}

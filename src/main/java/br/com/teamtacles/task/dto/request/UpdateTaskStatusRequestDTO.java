package br.com.teamtacles.task.dto.request;

import br.com.teamtacles.task.enumeration.ETaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "UpdateTaskStatusRequest", description = "DTO for updating the status of a task.")
public class UpdateTaskStatusRequestDTO {

    @Schema(description = "The new status for the task.", example = "DONE", required = true)
    @NotNull(message = "The new status cannot be null.")
    private ETaskStatus newStatus;

    @Schema(description = "A comment to add upon task completion. Only used when moving to DONE status.", example = "Feature fully implemented and tested.")
    @Size(max = 300, message = "Completion comment cannot exceed 300 characters.")
    private String completionComment;
}

package br.com.teamtacles.task.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "TaskAssignmentsBulkDeleteRequest", description = "DTO for bulk deletion of task assignments.")
public class TaskAssignmentsBulkDeleteRequestDTO {

    @Schema(description = "A list of user IDs to be unassigned from the task.", required = true)
    @NotEmpty(message = "The list of user IDs cannot be empty")
    private Set<Long> userIds;
}

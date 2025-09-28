package br.com.teamtacles.task.dto.response;

import br.com.teamtacles.task.enumeration.ETaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "TaskResponse", description = "DTO for detailed task data.")
public class TaskResponseDTO {

    @Schema(description = "The unique identifier of the task.", example = "1")
    private Long id;

    @Schema(description = "The title of the task.", example = "Develop new login feature")
    private String title;

    @Schema(description = "A detailed description of the task.", example = "Implement the full authentication flow using JWT.")
    private String description;

    @Schema(description = "The current status of the task.", example = "IN_PROGRESS")
    private ETaskStatus status;

    @Schema(description = "The date and time when the task was created.")
    private OffsetDateTime createdAt;

    @Schema(description = "The due date for the task.")
    private OffsetDateTime dueDate;

    @Schema(description = "The ID of the project this task belongs to.", example = "25")
    private Long projectId;

    @Schema(description = "The ID of the user who owns the task.", example = "101")
    private Long ownerId;

    @Schema(description = "The set of users assigned to the task and their roles.")
    private Set<UserAssignmentResponseDTO> assignments;
}

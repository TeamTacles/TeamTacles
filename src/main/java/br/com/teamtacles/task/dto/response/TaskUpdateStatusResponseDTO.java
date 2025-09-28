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
@Schema(name = "TaskUpdateStatusResponseDTO", description = "DTO for detailed updated task data.")
public class TaskUpdateStatusResponseDTO {

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

    @Schema(description = "The date and time when the task was completed.")
    private OffsetDateTime completedAt;

    @Schema(description = "Completion comment for the task.", example = "Task completed successfully!")
    private String completionComment;
}

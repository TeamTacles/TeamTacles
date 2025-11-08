package br.com.teamtacles.task.dto.response;

import br.com.teamtacles.project.dto.response.ProjectResponseDTO;
import br.com.teamtacles.task.enumeration.ETaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "UserTaskResponse", description = "DTO representing a task from a user's perspective.")
public class UserTaskResponseDTO {

    @Schema(description = "The unique identifier of the task.", example = "1")
    private Long id;

    @Schema(description = "The title of the task.", example = "Edit Car")
    private String title;

    @Schema(description = "A brief description of the task.", example = "Audit of financial records for the fourth quarter.")
    private String description;

    @Schema(description = "", example = "TO_DO")
    private ETaskStatus taskStatus;

    @Schema(description = "The total number of task in the project.", example = "5")
    private OffsetDateTime dueDate;

    @Schema(description = "A list of usernames of the project members.")
    private ProjectResponseDTO project;
}

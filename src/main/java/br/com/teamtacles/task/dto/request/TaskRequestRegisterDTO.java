package br.com.teamtacles.task.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "TaskRegisterRequest", description = "DTO for registering a new task.")
public class TaskRequestRegisterDTO {

    @Schema(description = "The title of the task.", example = "Develop new login feature", required = true)
    @NotBlank(message = "Title cannot be null or empty.")
    @Size(max = 100)
    private String title;

    @Schema(description = "A detailed description of the task.", example = "Implement the full authentication flow using JWT, including token generation and validation.")
    @Size(max = 500)
    private String description;

    @Schema(description = "The due date for the task, including timezone.", example = "2024-12-31T23:59:59-03:00")
    @FutureOrPresent(message = "The due date must be in the future.")
    private OffsetDateTime dueDate;
}
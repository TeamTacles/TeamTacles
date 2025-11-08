package br.com.teamtacles.task.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "TaskUpdateRequest", description = "DTO for updating an existing task's details.")
public class TaskRequestUpdateDTO {

    @Schema(description = "The new title for the task.", example = "Enhance login feature with MFA")
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    String title;

    @Schema(description = "The new description for the task.", example = "Add support for multi-factor authentication using TOTP.")
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    String description;

    @Schema(description = "The new due date for the task.", example = "2025-01-31T23:59:59-03:00")
    @FutureOrPresent(message = "The due date must be in the future ")
    private OffsetDateTime dueDate;
}

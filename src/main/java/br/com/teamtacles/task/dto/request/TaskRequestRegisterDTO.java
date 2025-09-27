package br.com.teamtacles.task.dto.request;

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
public class TaskRequestRegisterDTO {
    @NotBlank(message = "Title cannot be null or empty.")
    @Size(max = 100)
    private String title;

    @Size(max = 500)
    private String description;

    @FutureOrPresent(message = "The due date must be in the future.")
    private OffsetDateTime dueDate;
}
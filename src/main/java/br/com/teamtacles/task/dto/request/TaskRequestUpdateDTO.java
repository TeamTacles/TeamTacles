package br.com.teamtacles.task.dto.request;


import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskRequestUpdateDTO {

    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    String title;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    String description;

    @FutureOrPresent(message = "The due date must be in the future ")
    private OffsetDateTime dueDate;
}

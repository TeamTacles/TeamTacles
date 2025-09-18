package br.com.teamtacles.task.dto.request;


import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class TaskRequestUpdateDTO {

    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    String title;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    String description;

    private OffsetDateTime dueDate;
}

package br.com.teamtacles.project.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProjectRequestUpdateDTO {

    @Size(min = 3, max = 50, message = "Title must be between 3 and 50 characters")
    private String title;

    @Size(max = 250, message = "Description cannot exceed 250 characters")
    private String description;

}

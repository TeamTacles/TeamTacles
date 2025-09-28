package br.com.teamtacles.project.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ProjectUpdateRequest", description = "DTO for updating an existing project.")
public class ProjectRequestUpdateDTO {

    @Schema(description = "The new title for the project.", example = "Website Redesign V2")
    @Size(min = 3, max = 50, message = "Title must be between 3 and 50 characters")
    private String title;

    @Schema(description = "The new description for the project.", example = "Phase 2 of the website overhaul, focusing on user experience.")
    @Size(max = 250, message = "Description cannot exceed 250 characters")
    private String description;

}

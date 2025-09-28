package br.com.teamtacles.project.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ProjectResponse", description = "DTO representing a project's main details.")
public class ProjectResponseDTO {

    @Schema(description = "The unique identifier of the project.", example = "1")
    private Long id;

    @Schema(description = "The title of the project.", example = "New Mobile App")
    private String title;

    @Schema(description = "A brief description of the project.", example = "Development of the new company mobile application.")
    private String description;
}

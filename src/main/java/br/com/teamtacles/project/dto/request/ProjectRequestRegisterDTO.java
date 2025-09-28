package br.com.teamtacles.project.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ProjectRegisterRequest", description = "DTO for registering a new project.")
public class ProjectRequestRegisterDTO {

    @Schema(description = "The title of the project.", example = "Website Redesign", required = true)
    @NotBlank(message = "Title cannot be null or empty.")
    @Size(min=3, max=50)
    private String title;

    @Schema(description = "A brief description of the project.", example = "Complete overhaul of the company's main website.")
    @Size(max=250)
    private String description;
}

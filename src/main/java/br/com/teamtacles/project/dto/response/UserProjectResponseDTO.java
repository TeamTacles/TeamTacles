package br.com.teamtacles.project.dto.response;

import br.com.teamtacles.project.enumeration.EProjectRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "UserProjectResponse", description = "DTO representing a project from a user's perspective, including their role.")
public class UserProjectResponseDTO {

    @Schema(description = "The unique identifier of the project.", example = "1")
    private Long id;

    @Schema(description = "The title of the project.", example = "Q4 Financial Auditing")
    private String title;

    @Schema(description = "A brief description of the project.", example = "Audit of financial records for the fourth quarter.")
    private String description;

    @Schema(description = "The user's role in this specific project.", example = "ADMIN")
    private EProjectRole projectRole;

    @Schema(description = "The total number of task in the project.", example = "5")
    private long taskCount;

    @Schema(description = "A list of usernames of the project members.")
    private List<String> memberNames;
}

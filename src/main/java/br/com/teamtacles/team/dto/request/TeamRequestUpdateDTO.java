package br.com.teamtacles.team.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "TeamUpdateRequest", description = "DTO for updating an existing team's name")
public class TeamRequestUpdateDTO {

    @Schema(description = "The new name for the team.", example = "Beta Testers")
    @Size(min = 3, max = 50)
    private String name;

    @Schema(description = "The new description for the team.", example = "New description for this project.")
    @Size(max = 250)
    private String description;
}
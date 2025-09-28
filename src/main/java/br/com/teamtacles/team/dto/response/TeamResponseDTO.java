package br.com.teamtacles.team.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "TeamResponse", description = "DTO for returning team details")
public class TeamResponseDTO {

    @Schema(description = "Unique identifier of the team.", example = "1")
    private Long id;

    @Schema(description = "Name of the team.", example = "Alpha Testers")
    private String name;

    @Schema(description = "Description of the team.", example = "The best team ever!")
    private String description;
}

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
@Schema(name = "TeamRegisterRequest", description = "DTO for creating a new team")
public class TeamRequestRegisterDTO {

    @Schema(description = "The name of the new team.", example = "Alpha Testers", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "The team name cannot be blank")
    @Size(min = 3, max = 50)
    private String name;

    @Schema(description = "The description of the new team.", example = "This project is very important.", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 250)
    private String description;
}

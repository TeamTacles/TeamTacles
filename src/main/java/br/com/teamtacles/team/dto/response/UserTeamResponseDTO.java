package br.com.teamtacles.team.dto.response;

import br.com.teamtacles.team.enumeration.ETeamRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "UserTeamResponse", description = "Summarized information about a team that the user is a member of")
public class UserTeamResponseDTO {

    @Schema(description = "Unique identifier of the team.", example = "15")
    private Long id;

    @Schema(description = "Name of the team.", example = "Project Phoenix")
    private String name;

    @Schema(description = "Brief description of the team.", example = "Team responsible for the Phoenix relaunch.")
    private String description;

    @Schema(description = "The role of the current user within this team.", example = "OWNER")
    private ETeamRole teamRole;

    @Schema(description = "The total number of members in the team.", example = "5")
    private long memberCount;

    @Schema(description = "A list of usernames of the team members.")
    private List<String> memberNames;
}

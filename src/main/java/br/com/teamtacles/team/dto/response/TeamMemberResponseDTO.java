package br.com.teamtacles.team.dto.response;

import br.com.teamtacles.team.enumeration.ETeamRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "TeamMemberResponse", description = "DTO for returning details of a team member")
public class TeamMemberResponseDTO {

    @Schema(description = "Unique identifier of the user.", example = "2")
    private Long userId;

    @Schema(description = "Username of the member.", example = "john.doe")
    private String username;

    @Schema(description = "Email of the member.", example = "john@gmail.com")
    private String email;

    @Schema(description = "Role of the member within the team.", example = "MEMBER")
    private ETeamRole teamRole;
}

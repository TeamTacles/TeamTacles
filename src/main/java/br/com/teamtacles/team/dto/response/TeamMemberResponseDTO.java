package br.com.teamtacles.team.dto.response;

import br.com.teamtacles.team.enumeration.ETeamRole;
import lombok.Data;

@Data
public class TeamMemberResponseDTO {
    private Long userId;
    private String username;
    private String email;
    private ETeamRole teamRole;
}

package br.com.teamtacles.team.dto.response;

import br.com.teamtacles.team.enumeration.ETeamRole;
import lombok.Data;

@Data
public class UserTeamResponseDTO {
    private Long id;
    private String name;
    private String description;
    private ETeamRole teamRole;
}

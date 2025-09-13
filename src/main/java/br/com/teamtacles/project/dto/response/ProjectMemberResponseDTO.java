package br.com.teamtacles.project.dto.response;

import br.com.teamtacles.project.enumeration.EProjectRole;
import br.com.teamtacles.team.enumeration.ETeamRole;
import lombok.Data;

@Data
public class ProjectMemberResponseDTO {
    private Long userId;
    private String username;
    private String email;
    private EProjectRole projectRole;
}

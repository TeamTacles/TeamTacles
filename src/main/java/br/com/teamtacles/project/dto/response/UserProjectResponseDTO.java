package br.com.teamtacles.project.dto.response;

import br.com.teamtacles.project.enumeration.EProjectRole;
import br.com.teamtacles.team.enumeration.ETeamRole;
import lombok.Data;

@Data
public class UserProjectResponseDTO {
    private Long id;
    private String title;
    private String description;
    private EProjectRole projectRole;
}

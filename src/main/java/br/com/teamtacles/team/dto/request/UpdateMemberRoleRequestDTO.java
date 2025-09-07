package br.com.teamtacles.team.dto.request;


import br.com.teamtacles.team.enumeration.ETeamRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateMemberRoleRequestDTO {

    @NotNull(message = "The new role cannot be null")
    private ETeamRole newRole;
}

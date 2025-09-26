package br.com.teamtacles.team.dto.request;


import br.com.teamtacles.team.enumeration.ETeamRole;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMemberRoleTeamRequestDTO {

    @NotNull(message = "The new role cannot be null")
    private ETeamRole newRole;
}

package br.com.teamtacles.team.dto.request;

import br.com.teamtacles.team.enumeration.ETeamRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InvitedMemberRequestDTO {

    @NotBlank(message = "Email is not null or empty.")
    @Email(message = "Email format is invalid.")
    private String email;

    @NotNull(message = "Role is not null.")
    private ETeamRole role;
}

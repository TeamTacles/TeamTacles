package br.com.teamtacles.team.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import br.com.teamtacles.team.enumeration.ETeamRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "InvitedMemberRequest", description = "DTO to invite a member to a resource (like a project or team)")
public class InvitedMemberRequestDTO {

    @Schema(description = "Email of the user to be invited.", example = "new.member@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Email is not null or empty.")
    @Email(message = "Email format is invalid.")
    private String email;

    @Schema(description = "Role to be assigned to the invited user.", example = "MEMBER", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Role is not null.")
    private ETeamRole role;
}
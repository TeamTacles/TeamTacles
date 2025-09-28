package br.com.teamtacles.team.dto.request;


import br.com.teamtacles.team.enumeration.ETeamRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "UpdateMemberRoleRequest", description = "DTO for updating a team member's role")
public class UpdateMemberRoleTeamRequestDTO {

    @Schema(description = "The new role for the team member.", example = "ADMIN", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "The new role cannot be null")
    private ETeamRole newRole;
}

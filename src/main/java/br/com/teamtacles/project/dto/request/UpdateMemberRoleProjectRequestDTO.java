package br.com.teamtacles.project.dto.request;

import br.com.teamtacles.project.enumeration.EProjectRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "UpdateProjectMemberRoleRequest", description = "DTO for updating a project member's role.")
public class UpdateMemberRoleProjectRequestDTO {

    @Schema(description = "The new role to be assigned to the project member.", example = "ADMIN", required = true)
    @NotNull(message = "The new role cannot be null")
    private EProjectRole newRole;
}

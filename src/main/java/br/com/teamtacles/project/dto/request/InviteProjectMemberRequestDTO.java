package br.com.teamtacles.project.dto.request;

import br.com.teamtacles.project.enumeration.EProjectRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "InviteProjectMemberRequest", description = "DTO for inviting a new member to a project.")
public class InviteProjectMemberRequestDTO {

    @Schema(description = "Email of the user to be invited.", example = "new.member@example.com", required = true)
    @NotBlank(message = "Email cannot be null or empty.")
    @Email(message = "Email format is invalid.")
    private String email;

    @Schema(description = "Role to be assigned to the new member.", example = "MEMBER", required = true)
    @NotNull(message = "Role cannot be null.")
    private EProjectRole role;
}
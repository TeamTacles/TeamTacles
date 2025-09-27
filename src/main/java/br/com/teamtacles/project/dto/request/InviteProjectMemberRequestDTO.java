package br.com.teamtacles.project.dto.request;

import br.com.teamtacles.project.enumeration.EProjectRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InviteProjectMemberRequestDTO {

    @NotBlank(message = "Email cannot be null or empty.")
    @Email(message = "Email format is invalid.")
    private String email;

    @NotNull(message = "Role cannot be null.")
    private EProjectRole role;
}
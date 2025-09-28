package br.com.teamtacles.team.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "TeamInviteRequest", description = "DTO for inviting a user to a team")
public class InvitedUserRequestDTO {

    @Schema(description = "Email of the user to be invited.", example = "member@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "The email cannot be blank!")
    @Email
    private String email;
}

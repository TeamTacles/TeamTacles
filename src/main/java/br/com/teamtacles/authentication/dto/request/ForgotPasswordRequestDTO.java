package br.com.teamtacles.authentication.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ForgotPasswordRequest", description = "DTO for requesting a password reset.")
public class ForgotPasswordRequestDTO {

    @Schema(description = "The email address associated with the account to reset the password for.", example = "user@example.com", required = true)
    @NotBlank(message = "E-mail is required.")
    @Email(message = "E-mail should be valid.")
    private String email;
}
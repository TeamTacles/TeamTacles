package br.com.teamtacles.authentication.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ResetPasswordRequest", description = "DTO for resetting the password using a token.")
public class ResetPasswordDTO {

    @Schema(description = "The token received via email to authorize the password reset.", example = "a1b2c3d4-e5f6-7890-g1h2-i3j4k5l6m7n8", required = true)
    @NotBlank(message = "Token is required.")
    private String token;

    @Schema(description = "The new password for the account.", example = "newStrongPassword123", required = true)
    @NotBlank(message = "The new password cannot be blank.")
    @Size(min = 5, max = 100, message = "The new password must be between 5 and 100 characters.")
    private String newPassword;

    @Schema(description = "The new password confirmation for the account.", example = "newStrongPassword123", required = true)
    @NotBlank(message = "The password confirmation cannot be blank.")
    @Size(min = 5, max = 100, message = "The new password confirm must be between 5 and 100 characters.")
    private String passwordConfirm;
}
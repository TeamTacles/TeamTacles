package br.com.teamtacles.authentication.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(name = "ResetPasswordWebDTO", description = "DTO for resetting the password using a token.")
public class ResetPasswordWebDTO {

    @Schema(description = "The token received via email to authorize the password reset.", example = "a1b2c3d4-e5f6-7890-g1h2-i3j4k5l6m7n8", required = true)
    @NotBlank
    private String token;

    @Schema(description = "The new password for the account.", example = "newStrongPassword123", required = true)
    @NotBlank(message = "The new password cannot be blank.")
    @Size(min = 6, message = "The new password must be at least 6 characters long.")
    private String newPassword;

    @Schema(description = "The new password confirmation for the account.", example = "newStrongPassword123", required = true)
    @NotBlank(message = "The password confirmation cannot be blank.")
    private String confirmPassword;
}
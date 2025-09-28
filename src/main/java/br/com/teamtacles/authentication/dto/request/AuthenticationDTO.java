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
@Schema(name = "AuthenticationRequest", description = "DTO for user authentication (login).")
public class AuthenticationDTO {

    @Schema(description = "User's registered email.", example = "user@example.com", required = true)
    @Email(message = "Email should be valid")
    private String email;

    @Schema(description = "User's password.", example = "password123", required = true)
    @NotBlank(message = "Password is mandatory")
    private String password;
}

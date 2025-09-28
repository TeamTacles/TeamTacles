package br.com.teamtacles.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "UserRegisterRequest", description = "DTO for new user registration")
public class UserRequestRegisterDTO {

    @Schema(description = "The user's desired username.", example = "new_user", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 50, message = "The username must not exceed 50 characters")
    @NotBlank(message = "The name cannot be blank!")
    private String username;

    @Schema(description = "The user's email address.", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 100)
    @NotBlank(message = "The email cannot be blank!")
    @Email(message = "The email must be valid!")
    private String email;

    @Schema(description = "The user's password.", example = "password123", minLength = 5, maxLength = 100, requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(min = 5, max = 100, message = "The password must contain between 5 and 100 characters")
    @NotBlank(message = "The password cannot be blank!")
    private String password;

    @Schema(description = "Password confirmation.", example = "password123", minLength = 5, maxLength = 100, requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(min = 5, max = 100, message = "The password Confirm must contain between 5 and 100 characters")
    @NotBlank(message = "The password confirmation cannot be blank!")
    private String passwordConfirm;
}

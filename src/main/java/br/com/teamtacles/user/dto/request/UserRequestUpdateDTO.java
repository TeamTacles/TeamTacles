package br.com.teamtacles.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "UserUpdateRequest", description = "DTO for updating an existing user's details")
public class UserRequestUpdateDTO {

    @Schema(description = "The user's new username. If not provided, it will not be updated.", example = "updated_user")
    @Size(max = 50, message = "The username must not exceed 50 characters")
    private String username;

    @Schema(description = "The user's new email address. If not provided, it will not be updated.", example = "new.email@example.com")
    @Size(max = 100)
    @Email(message = "The email must be valid!")
    private String email;

    @Schema(description = "The user's new password. If not provided, it will not be updated.", example = "newPassword456", minLength = 5, maxLength = 100)
    @Size(min = 5, max = 100, message = "The password must contain between 5 and 100 characters")
    private String password;

    @Schema(description = "Confirmation of the new password. Required if 'password' is provided.", example = "newPassword456", minLength = 5, maxLength = 100)
    @Size(min = 5, max = 100, message = "The password confirm must contain between 5 and 100 characters")
    private String passwordConfirm;
}


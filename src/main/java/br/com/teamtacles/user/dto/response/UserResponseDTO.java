package br.com.teamtacles.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "UserResponse", description = "DTO for returning user information")
public class UserResponseDTO{

    @Schema(description = "Unique identifier of the user.", example = "1")
    private Long id;

    @Schema(description = "The user's username.", example = "john.doe")
    private String username;

    @Schema(description = "The user's email address.", example = "john.doe@example.com")
    private String email;

    @Schema(description = "User performs onboarding", example = "false")
    private boolean onboardingCompleted;
}

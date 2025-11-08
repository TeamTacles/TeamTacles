package br.com.teamtacles.common.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "AuthenticationResponseDTO", description = "DTO for authentication response containing the JWT token.")
public class AuthenticationResponseDTO {

    @Schema(description = "Token for autentication", example = "a1b2c3d4-e5f6-7890-g1h2-i3j4k5l6m7n8")
    private String token;
}
package br.com.teamtacles.common.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "InviteTokenLinkResponseDTO", description = "DTO for invite token response containing the token and its expiration time.")
public class InviteTokenLinkResponseDTO {

    @Schema(description = "Generated invite token", example = "abc123def456")
    private String inviteToken;

    @Schema(description = "Expiration date and time of the invite link", example = "2024-12-31T23:59:59Z")
    private OffsetDateTime expiresAt;
}

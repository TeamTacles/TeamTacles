package br.com.teamtacles.common.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "InviteLinkResponseDTO", description = "DTO for invite link response containing the link and its expiration time.")
public class InviteLinkResponseDTO {

    @Schema(description = "Generated invite link", example = "https://teamtacles.com/invite/abc123def456")
    private String inviteLink;

    @Schema(description = "Expiration date and time of the invite link", example = "2024-12-31T23:59:59Z")
    private OffsetDateTime expiresAt;
}

package br.com.teamtacles.common.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InviteLinkResponseDTO {
    private String inviteLink;
    private LocalDateTime expiresAt;
}

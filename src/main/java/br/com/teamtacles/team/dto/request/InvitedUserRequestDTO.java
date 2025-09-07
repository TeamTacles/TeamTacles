package br.com.teamtacles.team.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InvitedUserRequestDTO {
    @NotBlank
    @Email
    private String email;
}

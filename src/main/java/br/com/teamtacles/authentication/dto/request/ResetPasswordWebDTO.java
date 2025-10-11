package br.com.teamtacles.authentication.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordWebDTO {

    @NotBlank
    private String token;

    @NotBlank(message = "A nova senha não pode estar em branco.")
    @Size(min = 6, message = "A senha deve ter pelo menos 6 caracteres.")
    private String newPassword;

    @NotBlank(message = "A confirmação da senha não pode estar em branco.")
    private String confirmPassword;
}
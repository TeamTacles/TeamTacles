package br.com.teamtacles.authentication.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordRequestDTO {
    @NotBlank(message = "E-mail is required.")
    @Email(message = "E-mail should be valid.")
    private String email;
}
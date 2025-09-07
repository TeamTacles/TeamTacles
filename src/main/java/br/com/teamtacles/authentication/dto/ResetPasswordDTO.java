package br.com.teamtacles.authentication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordDTO {
    @NotBlank(message = "Token is required.")
    private String token;

    @NotBlank(message = "The new password cannot be blank.")
    @Size(min = 5, max = 100, message = "The new password must be between 5 and 100 characters.")
    private String newPassword;

    @NotBlank(message = "The password confirmation cannot be blank.")
    private String passwordConfirm;
}
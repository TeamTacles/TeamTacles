package br.com.teamtacles.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRequestRegisterDTO {

    @Size(max = 50, message = "The username must not exceed 50 characters")
    @NotBlank(message = "The name cannot be blank!")
    private String username;

    @Size(max = 100)
    @NotBlank(message = "The email cannot be blank!")
    @Email(message = "The email must be valid!")
    private String email;

    @Size(min = 5, max = 100, message = "The password must contain between 5 and 100 characters")
    @NotBlank(message = "The password cannot be blank!")
    private String password;

    @Size(min = 5, max = 100, message = "The password Confirm must contain between 5 and 100 characters")
    @NotBlank(message = "The password confirmation cannot be blank!")
    private String passwordConfirm;
}

package br.com.teamtacles.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRequestUpdateDTO {

    @Size(max = 50, message = "The username must not exceed 50 characters")
    private String username;

    @Size(max = 100)
    @Email(message = "The email must be valid!")
    private String email;

    @Size(min = 5, max = 100, message = "The password must contain between 5 and 100 characters")
    private String password;

    @Size(min = 5, max = 100, message = "The password confirm must contain between 5 and 100 characters")
    private String passwordConfirm;
}


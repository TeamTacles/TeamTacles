package br.com.teamtacles.user.validator;

import br.com.teamtacles.user.dto.request.UserRequestUpdateDTO;
import br.com.teamtacles.user.model.User;
import org.springframework.stereotype.Component;

@Component
public class PasswordUpdateValidator {

    private final PasswordMatchValidator passwordMatchValidator;
    private final NewPasswordValidator newPasswordValidator;

    public PasswordUpdateValidator(PasswordMatchValidator passwordMatchValidator, NewPasswordValidator newPasswordValidator) {
        this.passwordMatchValidator = passwordMatchValidator;
        this.newPasswordValidator = newPasswordValidator;
    }

    public void validate(UserRequestUpdateDTO userRequestDTO, User user) {
        String newPassword = userRequestDTO.getPassword();
        String passwordConfirm = userRequestDTO.getPasswordConfirm();

        if ((newPassword == null || newPassword.isBlank()) &&
                (passwordConfirm == null || passwordConfirm.isBlank())) {
            return;
        }

        if (newPassword == null || newPassword.isBlank() ||
                passwordConfirm == null || passwordConfirm.isBlank()) {
            throw new IllegalArgumentException("To change the password, you must provide both the new password and its confirmation.");
        }

        passwordMatchValidator.validate(newPassword, passwordConfirm);
        newPasswordValidator.validate(newPassword, user.getPassword());
    }
}
package br.com.teamtacles.user.validator;

import br.com.teamtacles.common.exception.SameAsCurrentPasswordException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class NewPasswordValidator {

    private final PasswordEncoder passwordEncoder;

    public NewPasswordValidator(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public void validate(String newPassword, String currentPasswordHash) {
        if (passwordEncoder.matches(newPassword, currentPasswordHash)) {
            throw new SameAsCurrentPasswordException("The new password cannot be the same as the current one.");
        }
    }
}
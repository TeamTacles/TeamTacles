package br.com.teamtacles.user.validator;

import br.com.teamtacles.common.exception.PasswordMismatchException;
import org.springframework.stereotype.Component;

// pra verificar se as senahs coincidem tanto no cadastro quanto no update
@Component
public class PasswordMatchValidator {
    public void validate(String password, String passwordConfirm) {
        if (password == null || passwordConfirm == null || !password.equals(passwordConfirm)) {
            throw new PasswordMismatchException("Password and confirmation don't match");
        }
    }
}
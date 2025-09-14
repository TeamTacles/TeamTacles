package br.com.teamtacles.user.validator;

import br.com.teamtacles.common.exception.ResourceNotFoundException;
import br.com.teamtacles.user.model.User;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class UserTokenValidator {

    public void validatePasswordResetToken(User user) {
        if (user.getResetPasswordToken() == null || user.getResetPasswordTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new ResourceNotFoundException("Validation token is invalid or has expired.");
        }
    }

    public void validateVerificationToken(User user) {
        if (user.getVerificationToken() == null || user.getVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new ResourceNotFoundException("The verification token has expired.");
        }
    }
}

package br.com.teamtacles.authentication.service;

import br.com.teamtacles.security.UserAuthenticated;
import br.com.teamtacles.user.model.User;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import br.com.teamtacles.common.service.EmailService;
import br.com.teamtacles.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthenticationService {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public AuthenticationService(JwtService jwtService, UserRepository userRepository, EmailService emailService) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    public String generateToken(Authentication authentication) {
        UserAuthenticated userAuthenticated = (UserAuthenticated) authentication.getPrincipal();
        User user = userAuthenticated.getUser();
        return jwtService.generateToken(user);
    }

    public void processForgotPasswordRequest(String email) {
        Optional<User> userOptional = userRepository.findByEmailIgnoreCase(email);

        userOptional.ifPresent(user -> {
            User updatedUser = createPasswordResetTokenForUser(user);
            emailService.sendPasswordResetEmail(updatedUser.getEmail(), updatedUser.getResetPasswordToken());
        });
    }

    @Transactional
    public User createPasswordResetTokenForUser(User user) {
        String token = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(1);

        user.setResetPasswordToken(token);
        user.setResetPasswordTokenExpiry(expiryDate);
        return userRepository.save(user);
    }
}
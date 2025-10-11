package br.com.teamtacles.authentication.service;

import br.com.teamtacles.common.dto.response.AuthenticationResponseDTO;
import br.com.teamtacles.security.UserAuthenticated;
import br.com.teamtacles.user.model.User;
import br.com.teamtacles.infrastructure.email.EmailService;
import br.com.teamtacles.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.web.forgot-password-url}")
    private String webResetPasswordUrl;

    public AuthenticationService(
            JwtService jwtService,
            UserRepository userRepository,
            EmailService emailService,
            PasswordEncoder passwordEncoder) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }


    public AuthenticationResponseDTO generateToken(Authentication authentication) {
        UserAuthenticated userAuthenticated = (UserAuthenticated) authentication.getPrincipal();
        User user = userAuthenticated.getUser();
        String token = jwtService.generateToken(user);
        return new AuthenticationResponseDTO(token);
    }


    @Transactional
    public void processForgotPasswordRequest(String email) {
        Optional<User> userOptional = userRepository.findByEmailIgnoreCase(email);

        userOptional.ifPresent(user -> {
            // Bloqueia reset para contas não verificadas
            if (!user.isEnabled()) {
                log.info("[SECURITY] Password reset blocked for unverified account. User ID: {}", user.getId());
                return;
            }

            user.clearPasswordResetToken();

            String token = UUID.randomUUID().toString();
            user.assignPasswordResetToken(token, LocalDateTime.now().plusHours(1));
            userRepository.save(user);

            String resetUrl = webResetPasswordUrl + "?token=" + token;
            emailService.sendPasswordResetEmail(user.getEmail(), resetUrl);

            log.info("[SECURITY] Password reset email sent to verified user. User ID: {}", user.getId());
        });
    }


    @Transactional
    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByResetPasswordToken(token)
                .orElseThrow(() -> new RuntimeException("Token inválido ou já utilizado."));

        if (user.getResetPasswordTokenExpiry().isBefore(LocalDateTime.now())) {
            log.warn("[SECURITY] Attempt to use expired reset token. User ID: {}", user.getId());
            throw new RuntimeException("O link de redefinição de senha expirou.");
        }

        user.updatePassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("[SECURITY] Password successfully reset. User ID: {}", user.getId());
    }
}
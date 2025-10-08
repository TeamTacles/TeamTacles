package br.com.teamtacles.authentication.service;

import br.com.teamtacles.common.dto.response.AuthenticationResponseDTO;
import br.com.teamtacles.security.UserAuthenticated;
import br.com.teamtacles.user.model.User;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import br.com.teamtacles.infrastructure.email.EmailService;
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

    public AuthenticationResponseDTO generateToken(Authentication authentication) { // <-- 2. MUDE O TIPO DE RETORNO
        UserAuthenticated userAuthenticated = (UserAuthenticated) authentication.getPrincipal();
        User user = userAuthenticated.getUser();
        String token = jwtService.generateToken(user);
        return new AuthenticationResponseDTO(token);
    }

    @Transactional
    public void processForgotPasswordRequest(String email) {
        Optional<User> userOptional = userRepository.findByEmailIgnoreCase(email);

        userOptional.ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            LocalDateTime expiryDate = LocalDateTime.now().plusHours(1);

            user.assignPasswordResetToken(token, LocalDateTime.now().plusHours(1));

            userRepository.save(user);

            emailService.sendPasswordResetEmail(user.getEmail(), user.getResetPasswordToken());
        });
    }

}
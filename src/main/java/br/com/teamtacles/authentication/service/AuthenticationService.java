package br.com.teamtacles.authentication.service;

import br.com.teamtacles.security.UserAuthenticated;
import br.com.teamtacles.user.model.User;
import br.com.teamtacles.security.CustomJwtAuthenticationConverter;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    private final JwtService jwtService;

    public AuthenticationService(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public String generateToken(Authentication authentication) {
        UserAuthenticated userAuthenticated = (UserAuthenticated) authentication.getPrincipal();
        User user = userAuthenticated.getUser();
        return jwtService.generateToken(user);
    }

}

package br.com.teamtacles.service;

import br.com.teamtacles.exception.ResourceNotFoundException;
import br.com.teamtacles.model.User;
import br.com.teamtacles.model.UserAuthenticated;
import br.com.teamtacles.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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

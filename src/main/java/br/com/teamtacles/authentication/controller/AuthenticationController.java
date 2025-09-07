package br.com.teamtacles.authentication.controller;

import br.com.teamtacles.authentication.service.AuthenticationService;
import br.com.teamtacles.authentication.dto.AuthenticationDTO;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import br.com.teamtacles.authentication.dto.ForgotPasswordRequestDTO;
import br.com.teamtacles.authentication.dto.ResetPasswordDTO;
import br.com.teamtacles.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    private final AuthenticationManager authenticationManager;
    private final AuthenticationService authenticationService;
    private final UserService userService;

    public AuthenticationController(AuthenticationManager authenticationManager, AuthenticationService authenticationService, UserService userService) {
        this.authenticationManager = authenticationManager;
        this.authenticationService = authenticationService;
        this.userService = userService;

    }

    @PostMapping("authenticate")
    public String authenticate(@RequestBody AuthenticationDTO request) {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword());
        Authentication authentication = authenticationManager.authenticate(authToken);
        return authenticationService.generateToken(authentication);
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void forgotPassword(@RequestBody @Valid ForgotPasswordRequestDTO request) {
        authenticationService.processForgotPasswordRequest(request.getEmail());
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody @Valid ResetPasswordDTO request) {
        userService.resetPassword(request.getToken(), request.getNewPassword(), request.getPasswordConfirm());
        return ResponseEntity.ok("Password has been reset successfully.");
    }


}

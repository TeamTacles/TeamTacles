package br.com.teamtacles.authentication.controller;

import br.com.teamtacles.authentication.service.AuthenticationService;
import br.com.teamtacles.authentication.dto.request.AuthenticationDTO;
import br.com.teamtacles.common.dto.response.AuthenticationResponseDTO;
import br.com.teamtacles.common.dto.response.MessageResponseDTO;
import br.com.teamtacles.common.exception.ErrorResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import br.com.teamtacles.authentication.dto.request.ForgotPasswordRequestDTO;
import br.com.teamtacles.authentication.dto.request.ResetPasswordDTO;
import br.com.teamtacles.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Endpoints for user authentication and password management.")
public class AuthenticationController {

    private final AuthenticationManager authenticationManager;
    private final AuthenticationService authenticationService;
    private final UserService userService;

    public AuthenticationController(AuthenticationManager authenticationManager, AuthenticationService authenticationService, UserService userService) {
        this.authenticationManager = authenticationManager;
        this.authenticationService = authenticationService;
        this.userService = userService;

    }

    @Operation(summary = "Authenticate User", description = "Authenticates a user's credentials and returns a JWT token upon success.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentication successful, token returned.",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE, schema = @Schema(type = "string", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."))),
            @ApiResponse(responseCode = "400", description = "Invalid input data",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })

    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponseDTO> authenticate(@RequestBody AuthenticationDTO request) {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword());
        Authentication authentication = authenticationManager.authenticate(authToken);
        AuthenticationResponseDTO responseDTO = authenticationService.generateToken(authentication);
        return ResponseEntity.ok(responseDTO);
    }

    @Operation(summary = "Forgot Password", description = "Initiates the password reset process by sending an email with a reset token to the user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Password reset process accepted. An email will be sent if the user exists."),
            @ApiResponse(responseCode = "400", description = "Invalid email format",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User with the specified email not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void forgotPassword(@RequestBody @Valid ForgotPasswordRequestDTO request) {
        authenticationService.processForgotPasswordRequest(request.getEmail());
    }

    @Operation(summary = "Reset Password", description = "Resets the user's password using a valid token and a new password.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password has been reset successfully.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = MessageResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data, token is invalid/expired, or passwords do not match",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Token not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponseDTO> resetPassword(@RequestBody @Valid ResetPasswordDTO request) {
        userService.resetPassword(request.getToken(), request.getNewPassword(), request.getPasswordConfirm());
        return ResponseEntity.ok(new MessageResponseDTO("Password has been reset successfully."));
    }

    @Operation(summary = "Resend Verification Email", description = "Resends the account verification email to a user who has not yet verified their account.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Verification email resent successfully.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = MessageResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid email format or user is already verified",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User with the specified email not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/resend-verification")
    public ResponseEntity<MessageResponseDTO> resendVerification(@RequestBody @Valid ForgotPasswordRequestDTO request) {
        userService.resendVerificationEmail(request.getEmail());
        return ResponseEntity.ok(new MessageResponseDTO("If the email is registered, a verification email has been sent."));
    }
}

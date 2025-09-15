package br.com.teamtacles.user.controller;

import br.com.teamtacles.common.dto.response.MessageResponseDTO;
import br.com.teamtacles.security.UserAuthenticated;
import br.com.teamtacles.user.dto.request.UserRequestRegisterDTO;
import br.com.teamtacles.user.dto.request.UserRequestUpdateDTO;
import br.com.teamtacles.user.dto.response.UserResponseDTO;

import br.com.teamtacles.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService){
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<MessageResponseDTO> registerUser(@RequestBody @Valid UserRequestRegisterDTO userRequestRegisterDTO) {
        userService.createUser(userRequestRegisterDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new MessageResponseDTO("User registered successfully. Please check your email to verify your account."));
    }

    @GetMapping("/verify-account")
    public ResponseEntity<MessageResponseDTO> verifyAccount(@RequestParam("token") String token) {
        userService.verifyUser(token);
        return ResponseEntity.ok(new MessageResponseDTO("Your account has been successfully verified."));
    }

    @GetMapping
    public ResponseEntity<UserResponseDTO> getUserById (@AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        UserResponseDTO userDTO = userService.getUserById(authenticatedUser.getUser());
        return ResponseEntity.status(HttpStatus.OK).body(userDTO);
    }

    @PatchMapping
    ResponseEntity<UserResponseDTO> updateUser(@RequestBody @Valid UserRequestUpdateDTO userRequestDTO, @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        UserResponseDTO userResponseDTO = userService.updateUser(userRequestDTO, authenticatedUser.getUser());
        return ResponseEntity.status(HttpStatus.OK).body(userResponseDTO);
    }

    @DeleteMapping
    ResponseEntity<Void> updateUser(@AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        userService.deleteUser(authenticatedUser.getUser());
        return ResponseEntity.noContent().build();
    }
}

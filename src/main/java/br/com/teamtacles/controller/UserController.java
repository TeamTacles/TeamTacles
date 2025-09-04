package br.com.teamtacles.controller;

import br.com.teamtacles.dto.request.user.UserRequestRegisterDTO;
import br.com.teamtacles.dto.request.user.UserRequestUpdateDTO;
import br.com.teamtacles.dto.response.user.UserResponseDTO;
import br.com.teamtacles.model.UserAuthenticated;
import br.com.teamtacles.service.UserService;
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
    public ResponseEntity<UserResponseDTO> registerUser(@RequestBody @Valid UserRequestRegisterDTO userRequestRegisterDTO) {
        UserResponseDTO userCreated = userService.createUser(userRequestRegisterDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(userCreated);
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

package br.com.teamtacles.user.controller;

import br.com.teamtacles.common.dto.response.MessageResponseDTO;
import br.com.teamtacles.common.exception.ErrorResponse;
import br.com.teamtacles.orchestration.service.UserAccountService;
import br.com.teamtacles.security.UserAuthenticated;
import br.com.teamtacles.user.dto.request.UserRequestRegisterDTO;
import br.com.teamtacles.user.dto.request.UserRequestUpdateDTO;
import br.com.teamtacles.user.dto.response.UserResponseDTO;

import br.com.teamtacles.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@Tag(name = "User Management", description = "Endpoints for user registration, verification, and profile management.")
public class UserController {

    private final UserService userService;
    private final UserAccountService userAccountService;

    public UserController(UserService userService, UserAccountService userAccountService){
        this.userService = userService;
        this.userAccountService = userAccountService;
    }

    @Operation(summary = "Register a new user", description = "Creates a new user account in the system and sends a verification email.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = MessageResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data, such as passwords not matching",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Conflict, username or email already exists",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<MessageResponseDTO> registerUser(@RequestBody @Valid UserRequestRegisterDTO userRequestRegisterDTO) {
        userService.createUser(userRequestRegisterDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new MessageResponseDTO("User registered successfully. Please check your email to verify your account."));
    }

    @Operation(summary = "Update user profile", description = "Updates the username, email, or password of the currently authenticated user.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User updated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = UserResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input, e.g., passwords do not match or new password is the same as the old one",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized if the user is not authenticated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Conflict, new username or email already exists",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping
    ResponseEntity<UserResponseDTO> updateUser(@RequestBody @Valid UserRequestUpdateDTO userRequestDTO, @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        UserResponseDTO userResponseDTO = userService.updateUser(userRequestDTO, authenticatedUser.getUser());
        return ResponseEntity.status(HttpStatus.OK).body(userResponseDTO);
    }

    @Operation(summary = "Get current user profile", description = "Retrieves the profile information of the currently authenticated user.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved user data",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = UserResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized if the user is not authenticated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<UserResponseDTO> getUserById (@AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        UserResponseDTO userDTO = userService.getUserById(authenticatedUser.getUser());
        return ResponseEntity.status(HttpStatus.OK).body(userDTO);
    }

    @Operation(summary = "Delete user account", description = "Permanently deletes the account of the currently authenticated user.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized if the user is not authenticated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping
    ResponseEntity<Void> updateUser(@AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        userAccountService.handleUserDeletion(authenticatedUser.getUser());
        return ResponseEntity.noContent().build();
    }
}

package br.com.teamtacles.user.service;

import br.com.teamtacles.common.exception.EmailAlreadyExistsException;
import br.com.teamtacles.common.exception.PasswordMismatchException;
import br.com.teamtacles.common.exception.ResourceNotFoundException;
import br.com.teamtacles.common.exception.UsernameAlreadyExistsException;
import br.com.teamtacles.common.service.EmailService;
import br.com.teamtacles.user.dto.request.UserRequestRegisterDTO;
import br.com.teamtacles.user.dto.response.UserResponseDTO;
import br.com.teamtacles.user.model.Role;
import br.com.teamtacles.user.model.User;
import br.com.teamtacles.user.repository.RoleRepository;
import br.com.teamtacles.user.repository.UserRepository;
import br.com.teamtacles.user.validator.PasswordMatchValidator;
import br.com.teamtacles.user.validator.UserTokenValidator;
import br.com.teamtacles.user.validator.UserUniquenessValidator;
import br.com.teamtacles.utils.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;


import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;




@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserUniquenessValidator userUniquenessValidator;

    @Mock
    private PasswordMatchValidator passwordMatchValidator;

    @Mock
    private UserTokenValidator userTokenValidator;

    @Mock
    private EmailService emailService;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private UserService userService;

    private UserRequestRegisterDTO validRequestDTO;
    private Role defaultRole;

    @BeforeEach
    void setUp() {
        validRequestDTO = TestDataFactory.createValidUserRequestRegisterDTO();
        defaultRole = TestDataFactory.createDefaultUserRole();
    }


    // 1. USER CREATION

    @Test
    @DisplayName("1.1- shouldCreateUserSuccessfully_WhenDataIsValid")
    void shouldCreateUserSuccessfully_WhenDataIsValid() {

        when(roleRepository.findByRoleName(any())).thenReturn(Optional.of(defaultRole));
        when(passwordEncoder.encode(validRequestDTO.getPassword())).thenReturn("encodedPassword123");

        ArgumentCaptor<User> userArgumentCaptor = ArgumentCaptor.forClass(User.class);

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(modelMapper.map(any(User.class), eq(UserResponseDTO.class)))
                .thenAnswer(invocation -> {
                    User savedUser = invocation.getArgument(0);
                    return new UserResponseDTO(1L, savedUser.getUsername(), savedUser.getEmail());
                });

        userService.createUser(validRequestDTO);

        verify(userRepository, times(1)).save(userArgumentCaptor.capture());

        User savedUser = userArgumentCaptor.getValue();

        assertEquals("encodedPassword123", savedUser.getPassword());

        assertFalse(savedUser.isEnabled());

        assertNotNull(savedUser.getVerificationToken());
        assertNotNull(savedUser.getVerificationTokenExpiry());

        verify(userUniquenessValidator, times(1)).validate(validRequestDTO);
        verify(passwordMatchValidator, times(1)).validate(validRequestDTO.getPassword(), validRequestDTO.getPasswordConfirm());

        verify(emailService, times(1)).sendVerificationEmail(
                eq(savedUser.getEmail()),
                eq(savedUser.getVerificationToken())
        );

    }

    @Test
    @DisplayName("1.2- shouldThrowException_WhenAttemptingToCreateUserWithExistingUsername")
    void shouldThrowException_WhenAttemptingToCreateUserWithExistingUsername() {
        doThrow(new UsernameAlreadyExistsException("Username 'testuser' already exists")).when(userUniquenessValidator).validate(any(UserRequestRegisterDTO.class));

        assertThrows(UsernameAlreadyExistsException.class, () -> {
            userService.createUser(validRequestDTO);
        });

        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString());


    }

    @Test
    @DisplayName("1.3 - shouldThrowException_WhenAttemptingToCreateUserWithExistingEmail")
    void shouldThrowException_WhenAttemptingToCreateUserWithExistingEmail() {

        doThrow(new EmailAlreadyExistsException("Email 'test@gmail.com' already exists")).when(userUniquenessValidator).validate(any(UserRequestRegisterDTO.class));

        assertThrows(EmailAlreadyExistsException.class, () -> {
            userService.createUser(validRequestDTO);
        });

        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("1.4- ShouldThrowException_WhenPasswordsDoNotMatchOnRegistration")
    void shouldThrowException_WhenPasswordsDoNotMatchOnRegistration() {
        doThrow(new PasswordMismatchException("Password and confirmation dont match"))
                .when(passwordMatchValidator)
                .validate(validRequestDTO.getPassword(), validRequestDTO.getPasswordConfirm());

        assertThrows(PasswordMismatchException.class, () -> {
            userService.createUser(validRequestDTO);
        });

        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString());


    }

    @Test
    @DisplayName("1.5 - ShouldThrowException_WhenDefaultUserRoleIsNotFoundOnCreation")
    void shouldThrowException_WhenDefaultUserRoleIsNotFoundOnCreation() {

        when(roleRepository.findByRoleName(any())).thenReturn(Optional.empty());

        doNothing().when(userUniquenessValidator).validate(any());
        doNothing().when(passwordMatchValidator).validate(any(), any());

        assertThrows(ResourceNotFoundException.class, () -> {
            userService.createUser(validRequestDTO);
        });

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
    }

    // ACCOUNT VERIFICATION/CONFIRMATION

    @Test
    @DisplayName("2.1- ShouldVerifyUserSuccessfully_WhenTokenIsValidAndNotExpired")
    void shouldVerifyUserSucessfully_WhenTokenIsValidAndNotExpired() {


        User unverifiedUser = TestDataFactory.createUnverifiedUser();
        String validToken = unverifiedUser.getVerificationToken();

        when(userRepository.findByVerificationToken(validToken)).thenReturn(Optional.of(unverifiedUser));
        doNothing().when(userTokenValidator).validateVerificationToken(any(User.class));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        userService.verifyUser(validToken);

        verify(userRepository, times(1)).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertTrue(savedUser.isEnabled(), "User must be enabled");
        assertNull(savedUser.getVerificationToken(), "The verification token must be null");
        assertNull(savedUser.getVerificationTokenExpiry(), "The date and time must be null");

    }

    @Test
    @DisplayName("2.2 ShouldThrowException_WhenVerificationTokenIsInvalidOrNonExistent")
    void shouldThrowException_WhenVerificationTokenIsInvalidOrNonExistent() {
        String invalidToken = "non-existent-token-123";

        when(userRepository.findByVerificationToken(invalidToken)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            userService.verifyUser(invalidToken);
        });

        verify(userRepository, never()).save(any(User.class));

    }

    @Test
    @DisplayName("2.3 - ShouldThrowException_WhenVerificationTokenIsExpired")
    void shouldThrowException_WhenVerificationTokenIsExpired() {

        User userWithExpiredToken = TestDataFactory.createUnverifiedUser();
        userWithExpiredToken.setVerificationTokenExpiry(LocalDateTime.now().minusHours(1));
        String expiredToken = userWithExpiredToken.getVerificationToken();

        when(userRepository.findByVerificationToken(expiredToken)).thenReturn(Optional.of(userWithExpiredToken));

        doThrow(new ResourceNotFoundException("The verification token has expired."))
                .when(userTokenValidator)
                .validateVerificationToken(any(User.class));

        assertThrows(ResourceNotFoundException.class, () -> {
            userService.verifyUser(expiredToken);
        });

        verify(userRepository, never()).save(any(User.class));



    }

}

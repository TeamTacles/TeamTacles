package br.com.teamtacles.user.service;

import br.com.teamtacles.common.exception.*;
import br.com.teamtacles.infrastructure.email.EmailService;
import br.com.teamtacles.user.dto.request.UserRequestRegisterDTO;
import br.com.teamtacles.user.dto.request.UserRequestUpdateDTO;
import br.com.teamtacles.user.dto.response.UserResponseDTO;
import br.com.teamtacles.user.model.Role;
import br.com.teamtacles.user.model.User;
import br.com.teamtacles.user.repository.RoleRepository;
import br.com.teamtacles.user.repository.UserRepository;
import br.com.teamtacles.user.validator.*;
import br.com.teamtacles.utils.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
    private NewPasswordValidator newPasswordValidator;
    @Mock
    private UserTokenValidator userTokenValidator;
    @Mock
    private EmailService emailService;
    @Mock
    private ModelMapper modelMapper;
    @Mock
    private PasswordUpdateValidator passwordUpdateValidator;
    @InjectMocks
    private UserService userService;

    // 1. USER CREATION
    @Nested
    @DisplayName("1. User Creation Tests")
    class UserCreationTests {
        private UserRequestRegisterDTO validRequestDTO;
        private Role defaultRole;

        @BeforeEach
        void setUp() {
            validRequestDTO = TestDataFactory.createValidUserRequestRegisterDTO();
            defaultRole = TestDataFactory.createDefaultUserRole();
        }

        @Test
        @DisplayName("1.1 - shouldCreateUserSuccessfully_WhenDataIsValid")
        void shouldCreateUserSuccessfully_WhenDataIsValid() {
            // Given
            when(roleRepository.findByRoleName(any())).thenReturn(Optional.of(defaultRole));
            when(passwordEncoder.encode(validRequestDTO.getPassword())).thenReturn("encodedPassword123");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(modelMapper.map(any(User.class), eq(UserResponseDTO.class)))
                    .thenAnswer(invocation -> {
                        User savedUser = invocation.getArgument(0);
                        return new UserResponseDTO(1L, savedUser.getUsername(), savedUser.getEmail());
                    });

            ArgumentCaptor<User> userArgumentCaptor = ArgumentCaptor.forClass(User.class);

            // When
            userService.createUser(validRequestDTO);

            // Then
            verify(userRepository, times(1)).save(userArgumentCaptor.capture());
            User savedUser = userArgumentCaptor.getValue();

            assertThat(savedUser.getPassword()).isEqualTo("encodedPassword123");
            assertThat(savedUser.isEnabled()).isFalse();
            assertThat(savedUser.getVerificationToken()).isNotNull();
            assertThat(savedUser.getVerificationTokenExpiry()).isNotNull();

            verify(userUniquenessValidator, times(1)).validate(validRequestDTO);
            verify(passwordMatchValidator, times(1)).validate(validRequestDTO.getPassword(), validRequestDTO.getPasswordConfirm());
            verify(emailService, times(1)).sendVerificationEmail(
                    eq(savedUser.getEmail()),
                    eq(savedUser.getVerificationToken())
            );
        }

        @Test
        @DisplayName("1.2 - shouldThrowException_WhenAttemptingToCreateUserWithExistingUsername")
        void shouldThrowException_WhenAttemptingToCreateUserWithExistingUsername() {
            // Given
            doThrow(new UsernameAlreadyExistsException("Username 'testuser' already exists")).when(userUniquenessValidator).validate(any(UserRequestRegisterDTO.class));

            // When & Then
            assertThatThrownBy(() -> userService.createUser(validRequestDTO))
                    .isInstanceOf(UsernameAlreadyExistsException.class)
                    .hasMessage("Username 'testuser' already exists");

            verify(userRepository, never()).save(any(User.class));
            verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
        }

        @Test
        @DisplayName("1.3 - shouldThrowException_WhenAttemptingToCreateUserWithExistingEmail")
        void shouldThrowException_WhenAttemptingToCreateUserWithExistingEmail() {
            // Given
            doThrow(new EmailAlreadyExistsException("Email 'test@gmail.com' already exists")).when(userUniquenessValidator).validate(any(UserRequestRegisterDTO.class));

            // When & Then
            assertThatThrownBy(() -> userService.createUser(validRequestDTO))
                    .isInstanceOf(EmailAlreadyExistsException.class)
                    .hasMessage("Email 'test@gmail.com' already exists");

            verify(userRepository, never()).save(any(User.class));
            verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
        }

        @Test
        @DisplayName("1.4 - shouldThrowException_WhenPasswordsDoNotMatchOnRegistration")
        void shouldThrowException_WhenPasswordsDoNotMatchOnRegistration() {
            // Given
            doThrow(new PasswordMismatchException("Password and confirmation dont match"))
                    .when(passwordMatchValidator)
                    .validate(validRequestDTO.getPassword(), validRequestDTO.getPasswordConfirm());

            // When & Then
            assertThatThrownBy(() -> userService.createUser(validRequestDTO))
                    .isInstanceOf(PasswordMismatchException.class)
                    .hasMessage("Password and confirmation dont match");

            verify(userRepository, never()).save(any(User.class));
            verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
        }

        @Test
        @DisplayName("1.5 - shouldThrowException_WhenDefaultUserRoleIsNotFoundOnCreation")
        void shouldThrowException_WhenDefaultUserRoleIsNotFoundOnCreation() {
            // Given
            when(roleRepository.findByRoleName(any())).thenReturn(Optional.empty());
            doNothing().when(userUniquenessValidator).validate(any());
            doNothing().when(passwordMatchValidator).validate(any(), any());

            // When & Then
            assertThatThrownBy(() -> userService.createUser(validRequestDTO))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(passwordEncoder, never()).encode(anyString());
            verify(userRepository, never()).save(any(User.class));
            verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
        }
    }

    // ACCOUNT VERIFICATION/CONFIRMATION
    @Nested
    @DisplayName("2. Account Verification Tests")
    class AccountVerificationTests {

        @Test
        @DisplayName("2.1 - shouldVerifyUserSuccessfully_WhenTokenIsValidAndNotExpired")
        void shouldVerifyUserSuccessfully_WhenTokenIsValidAndNotExpired() {
            // Given
            User unverifiedUser = TestDataFactory.createUnverifiedUser();
            when(userRepository.findByVerificationToken(anyString())).thenReturn(Optional.of(unverifiedUser));
            doNothing().when(userTokenValidator).validateVerificationToken(any(User.class));

            // When
            userService.verifyUser("valid-token");

            // Then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();

            assertThat(savedUser.isEnabled()).isTrue();
            assertThat(savedUser.getVerificationToken()).isNull();
            assertThat(savedUser.getVerificationTokenExpiry()).isNull();
        }

        @Test
        @DisplayName("2.2 - shouldThrowException_WhenVerificationTokenIsInvalidOrNonExistent")
        void shouldThrowException_WhenVerificationTokenIsInvalidOrNonExistent() {
            // Given
            when(userRepository.findByVerificationToken(anyString())).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userService.verifyUser("invalid-token"))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("2.3 - shouldThrowException_WhenVerificationTokenIsExpired")
        void shouldThrowException_WhenVerificationTokenIsExpired() {
            // Given
            User userWithExpiredToken = TestDataFactory.createUnverifiedUser();
            when(userRepository.findByVerificationToken(anyString())).thenReturn(Optional.of(userWithExpiredToken));
            doThrow(new ResourceNotFoundException("Token expired")).when(userTokenValidator).validateVerificationToken(any(User.class));

            // When & Then
            assertThatThrownBy(() -> userService.verifyUser("expired-token"))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(userRepository, never()).save(any());
        }
    }

    // USER PROFILE UPDATE
    @Nested
    @DisplayName("3. User Profile Update Tests")
    class UserProfileUpdateTests {
        private User existingUser;

        @BeforeEach
        void setUp() {
            existingUser = TestDataFactory.createValidUser();
        }

        @Test
        @DisplayName("3.1 - shouldUpdateUsernameAndEmail_WhenDataIsValid")
        void shouldUpdateUsernameAndEmail_WhenDataIsValid() {
            // Given
            UserRequestUpdateDTO updateDTO = new UserRequestUpdateDTO("newUsername", "new@email.com", null, null);

            doNothing().when(userUniquenessValidator).validate(any(UserRequestUpdateDTO.class), anyLong());
            doNothing().when(passwordUpdateValidator).validate(any(UserRequestUpdateDTO.class), any(User.class));

            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(modelMapper.map(any(User.class), eq(UserResponseDTO.class)))
                    .thenReturn(new UserResponseDTO(existingUser.getId(), updateDTO.getUsername(), updateDTO.getEmail()));

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

            // When
            userService.updateUser(updateDTO, existingUser);

            // Then
            verify(userRepository).save(userCaptor.capture());
            User updatedUser = userCaptor.getValue();
            assertThat(updatedUser.getUsername()).isEqualTo("newUsername");
            assertThat(updatedUser.getEmail()).isEqualTo("new@email.com");
        }

        @Test
        @DisplayName("3.2 - shouldUpdatePassword_WhenNewPasswordAndConfirmationAreValid")
        void shouldUpdatePassword_WhenNewPasswordAndConfirmationAreValid() {
            // Given
            UserRequestUpdateDTO updateDTO = new UserRequestUpdateDTO(null, null, "newPassword123", "newPassword123");

            doNothing().when(userUniquenessValidator).validate(any(UserRequestUpdateDTO.class), anyLong());
            doNothing().when(passwordUpdateValidator).validate(any(UserRequestUpdateDTO.class), any(User.class));

            when(passwordEncoder.encode("newPassword123")).thenReturn("encodedNewPassword");
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

            // When
            userService.updateUser(updateDTO, existingUser);

            // Then
            verify(userRepository).save(userCaptor.capture());
            User updatedUser = userCaptor.getValue();
            assertThat(updatedUser.getPassword()).isEqualTo("encodedNewPassword");
        }

        @Test
        @DisplayName("3.3 - shouldThrowException_WhenUpdatingToAnExistingUsername")
        void shouldThrowException_WhenUpdatingToAnExistingUsername() {
            // Given
            UserRequestUpdateDTO updateDTO = new UserRequestUpdateDTO("existingUsername", null, null, null);

            doThrow(new UsernameAlreadyExistsException("Username already exists"))
                    .when(userUniquenessValidator)
                    .validate(updateDTO, existingUser.getId());

            // When & Then
            assertThatThrownBy(() -> userService.updateUser(updateDTO, existingUser))
                    .isInstanceOf(UsernameAlreadyExistsException.class);

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("3.4 - shouldThrowException_WhenUpdatingPasswordButConfirmationDoesNotMatch")
        void shouldThrowException_WhenUpdatingPasswordButConfirmationDoesNotMatch() {
            // Given
            UserRequestUpdateDTO updateDTO = new UserRequestUpdateDTO(null, null, "newPassword123", "wrongConfirmation");

            doThrow(new PasswordMismatchException("Passwords do not match"))
                    .when(passwordUpdateValidator)
                    .validate(updateDTO, existingUser);

            // When & Then
            assertThatThrownBy(() -> userService.updateUser(updateDTO, existingUser))
                    .isInstanceOf(PasswordMismatchException.class);

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("3.5 - shouldThrowException_WhenNewPasswordIsSameAsCurrentPassword")
        void shouldThrowException_WhenNewPasswordIsSameAsCurrentPassword() {
            // Given
            UserRequestUpdateDTO updateDTO = new UserRequestUpdateDTO(null, null, "currentPassword", "currentPassword");

            doThrow(new SameAsCurrentPasswordException("New password cannot be the same as the current one."))
                    .when(passwordUpdateValidator)
                    .validate(updateDTO, existingUser);

            // When & Then
            assertThatThrownBy(() -> userService.updateUser(updateDTO, existingUser))
                    .isInstanceOf(SameAsCurrentPasswordException.class);

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("3.6 - shouldThrowException_WhenUpdatingToAnExistingEmail")
        void shouldThrowException_WhenUpdatingToAnExistingEmail() {
            // Given
            UserRequestUpdateDTO updateDTO = new UserRequestUpdateDTO(null, "existing@email.com", null, null);

            doThrow(new EmailAlreadyExistsException("Email already exists"))
                    .when(userUniquenessValidator)
                    .validate(updateDTO, existingUser.getId());

            // When & Then
            assertThatThrownBy(() -> userService.updateUser(updateDTO, existingUser))
                    .isInstanceOf(EmailAlreadyExistsException.class);

            verify(userRepository, never()).save(any(User.class));
        }
    }

    // PASSWORD RESET
    @Nested
    @DisplayName("4. Password Reset Tests")
    class PasswordResetTests {

        @Test
        @DisplayName("4.1 - shouldResetPasswordSuccessfully_WhenTokenIsValid")
        void shouldResetPasswordSuccessfully_WhenTokenIsValid() {
            // Given
            User user = TestDataFactory.createValidUser();
            user.assignPasswordResetToken("valid-token", LocalDateTime.now().plusHours(1));
            String newPassword = "newSecurePassword123";

            when(userRepository.findByResetPasswordToken("valid-token")).thenReturn(Optional.of(user));
            doNothing().when(userTokenValidator).validatePasswordResetToken(user);
            doNothing().when(passwordMatchValidator).validate(newPassword, newPassword);
            doNothing().when(newPasswordValidator).validate(newPassword, user.getPassword());
            when(passwordEncoder.encode(newPassword)).thenReturn("encodedNewPassword");

            // When
            userService.resetPassword("valid-token", newPassword, newPassword);

            // Then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();

            assertThat(savedUser.getPassword()).isEqualTo("encodedNewPassword");
            assertThat(savedUser.getResetPasswordToken()).isNull();
            assertThat(savedUser.getResetPasswordTokenExpiry()).isNull();
        }

        @Test
        @DisplayName("4.2 - shouldThrowException_WhenResetTokenIsInvalidOrExpired")
        void shouldThrowException_WhenResetTokenIsInvalidOrExpired() {
            // Given
            String invalidToken = "invalid-token";
            when(userRepository.findByResetPasswordToken(invalidToken)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userService.resetPassword(invalidToken, "any", "any"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // USER DELETION
    @Nested
    @DisplayName("5. User Deletion Tests")
    class UserDeletionTests {

        @Test
        @DisplayName("5.1 - shouldDeleteUserSuccessfully_WhenRequestedByAuthenticatedUser")
        void shouldDeleteUserSuccessfully_WhenRequestedByAuthenticatedUser() {
            // Given
            User userToDelete = TestDataFactory.createValidUser();

            // When
            userService.deleteUser(userToDelete);

            // Then
            verify(userRepository, times(1)).delete(userToDelete);
        }
    }
}
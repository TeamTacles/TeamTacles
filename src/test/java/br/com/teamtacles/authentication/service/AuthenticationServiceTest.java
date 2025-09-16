package br.com.teamtacles.authentication.service;

import br.com.teamtacles.common.service.EmailService;
import br.com.teamtacles.security.UserAuthenticated;
import br.com.teamtacles.security.UserDetailService;
import br.com.teamtacles.user.model.User;
import br.com.teamtacles.user.repository.UserRepository;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthenticationServiceTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthenticationService authenticationService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = TestDataFactory.createValidUser();
    }

    @Nested
    @DisplayName("1. Authentication and Token Flow")
    class AuthFlowTests {
        @Test
        @DisplayName("1.1 - Should generate JWT when authentication is valid")
        void shouldGenerateJWT_WhenAuthenticationIsValid() {
            // Given
            UserAuthenticated userAuthenticated = new UserAuthenticated(testUser);
            Authentication authentication = new UsernamePasswordAuthenticationToken(userAuthenticated, null, userAuthenticated.getAuthorities());
            String expectedToken = "mocked.jwt.token";

            when(jwtService.generateToken(testUser)).thenReturn(expectedToken);

            // When
            String generatedToken = authenticationService.generateToken(authentication);

            // Then
            assertThat(generatedToken).isEqualTo(expectedToken);
        }

        @Test
        @DisplayName("1.2 - Should throw UsernameNotFoundException when user email does not exist (UserDetailService)")
        void shouldThrowException_WhenEmailDoesNotExist() {
            // Given
            String nonExistentEmail = "nonexistent@email.com";
            UserDetailService realUserDetailService = new UserDetailService(userRepository);
            when(userRepository.findByEmailIgnoreCase(nonExistentEmail)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> realUserDetailService.loadUserByUsername(nonExistentEmail))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage("User not found with email " + nonExistentEmail);
        }

        @Test
        @DisplayName("1.3 - Should identify an unverified (disabled) user (UserDetailService)")
        void shouldIdentifyDisabledAccount_WhenUserIsUnverified() {
            // Given
            User unverifiedUser = TestDataFactory.createUnverifiedUser();
            String email = unverifiedUser.getEmail();
            UserDetailService realUserDetailService = new UserDetailService(userRepository);
            when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(unverifiedUser));

            // When
            UserDetails userDetails = realUserDetailService.loadUserByUsername(email);

            // Then
            assertThat(userDetails.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("2. Forgot Password Flow")
    class ForgotPasswordTests {
        @Test
        @DisplayName("2.1 - Should process forgot password and send email when user exists")
        void shouldProcessForgotPassword_WhenUserExists() {
            // Arrange
            String email = testUser.getEmail();
            when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(testUser));

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

            // Act
            authenticationService.processForgotPasswordRequest(email);

            // Assert
            verify(userRepository, times(1)).save(userCaptor.capture());
            verify(emailService, times(1)).sendPasswordResetEmail(eq(email), anyString());

            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getResetPasswordToken()).isNotNull();
            assertThat(savedUser.getResetPasswordTokenExpiry()).isNotNull();
        }

        @Test
        @DisplayName("2.2 - Should not send email when user does not exist")
        void shouldNotSendEmail_WhenUserDoesNotExist() {
            // Given
            String nonExistentEmail = "nonexistent@email.com";
            when(userRepository.findByEmailIgnoreCase(nonExistentEmail)).thenReturn(Optional.empty());

            // When
            authenticationService.processForgotPasswordRequest(nonExistentEmail);

            // Then
            verify(userRepository, never()).save(any(User.class));
            verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
        }
    }
}
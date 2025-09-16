package br.com.teamtacles.user.service;

import br.com.teamtacles.common.exception.*;
import br.com.teamtacles.common.service.EmailService;
import br.com.teamtacles.user.dto.request.UserRequestRegisterDTO;
import br.com.teamtacles.user.dto.request.UserRequestUpdateDTO;
import br.com.teamtacles.user.dto.response.UserResponseDTO;
import br.com.teamtacles.user.enumeration.ERole;
import br.com.teamtacles.user.model.Role;
import br.com.teamtacles.user.model.User;
import br.com.teamtacles.user.repository.RoleRepository;
import br.com.teamtacles.user.repository.UserRepository;
import br.com.teamtacles.user.validator.NewPasswordValidator;
import br.com.teamtacles.user.validator.PasswordMatchValidator;
import br.com.teamtacles.user.validator.UserTokenValidator;
import br.com.teamtacles.user.validator.UserUniquenessValidator;
import org.springframework.transaction.annotation.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserTokenValidator userTokenValidator;
    private final NewPasswordValidator newPasswordValidator;
    private final PasswordMatchValidator passwordMatchValidator;
    private final UserUniquenessValidator userUniquenessValidator;

    private final EmailService emailService;

    private final ModelMapper modelMapper;

    public UserService(UserRepository userRepository,PasswordEncoder passwordEncoder,
                       ModelMapper modelMapper, RoleRepository roleRepository,
                       PasswordMatchValidator passwordMatchValidator, EmailService emailService,
                       UserUniquenessValidator userUniquenessValidator,
                       UserTokenValidator userTokenValidator,
                       NewPasswordValidator newPasswordValidator) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.modelMapper = modelMapper;
        this.passwordMatchValidator = passwordMatchValidator;
        this.emailService = emailService;
        this.userUniquenessValidator = userUniquenessValidator;
        this.userTokenValidator = userTokenValidator;
        this.newPasswordValidator = newPasswordValidator;
    }

    @Transactional
    public UserResponseDTO createUser(UserRequestRegisterDTO registerDTO) {
        userUniquenessValidator.validate(registerDTO);
        passwordMatchValidator.validate(registerDTO.getPassword(), registerDTO.getPasswordConfirm());

        Role userRole = roleRepository.findByRoleName(ERole.USER)
                .orElseThrow(() -> new ResourceNotFoundException("Error: Role USER not found."));

        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setEmail(registerDTO.getEmail());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));

        user.setRoles(Set.of(userRole));

        String token = UUID.randomUUID().toString();
        user.setVerificationToken(token);
        user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(1));
        user.setEnabled(false);

        User savedUser = userRepository.save(user);
        emailService.sendVerificationEmail(savedUser.getEmail(), token);

        return modelMapper.map(savedUser, UserResponseDTO.class);
    }

    @Transactional
    public UserResponseDTO updateUser(UserRequestUpdateDTO userRequestDTO, User user) {
        if (userRequestDTO.getUsername() != null && !userRequestDTO.getUsername().isBlank()) {
            user.setUsername(userRequestDTO.getUsername());
        }

        if (userRequestDTO.getEmail() != null && !userRequestDTO.getEmail().isBlank()) {
            user.setEmail(userRequestDTO.getEmail());
        }

        handlePasswordUpdate(userRequestDTO, user);

        User updatedUser = userRepository.save(user);
        return modelMapper.map(updatedUser, UserResponseDTO.class);
    }
    
    public UserResponseDTO getUserById(User user) {
        return modelMapper.map(user, UserResponseDTO.class);
    }

    @Transactional
    public void resetPassword(String token, String newPassword, String passwordConfirm) {
        passwordMatchValidator.validate(newPassword, passwordConfirm);

        User user = findByResetPasswordTokenOrThrow(token);

        userTokenValidator.validatePasswordResetToken(user);
        newPasswordValidator.validate(newPassword, user.getPassword());

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);

        userRepository.save(user);
    }

    @Transactional
    public void verifyUser(String token) {
        User user = findByVerificationTokenOrThrow(token);
        userTokenValidator.validateVerificationToken(user);

        user.setEnabled(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);

        userRepository.save(user);
    }

    @Transactional
    public void resendVerificationEmail(String email) {
        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            if (!user.isEnabled()) {

                String token = UUID.randomUUID().toString();
                user.setVerificationToken(token);
                user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(24));

                userRepository.save(user);
                emailService.sendVerificationEmail(user.getEmail(), token);
            }
        });
    }

    @Transactional
    public void deleteUser(User user) {
        userRepository.delete(user);
    }

    private void handlePasswordUpdate(UserRequestUpdateDTO userRequestDTO, User user) {
        String newPassword = userRequestDTO.getPassword();
        String passwordConfirm = userRequestDTO.getPasswordConfirm();

        if ((newPassword != null && !newPassword.isBlank()) ||
                (passwordConfirm != null && !passwordConfirm.isBlank()))  {

            if (newPassword == null || newPassword.isBlank() ||
                    passwordConfirm == null || passwordConfirm.isBlank()) {
                throw new IllegalArgumentException("To change the password, you must provide both the new password and its confirmation.");
            }

            passwordMatchValidator.validate(newPassword, passwordConfirm);
            newPasswordValidator.validate(newPassword, user.getPassword());

            user.setPassword(passwordEncoder.encode(newPassword));
        }
    }

    public User findUserEntityById(Long userId) {
        return findUserByIdOrThrow(userId);
    }

    public User findUserEntityByEmail(String email) {
        return findByEmailIgnoreCaseOrThrow(email);
    }

    private User findUserByIdOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    private User findByEmailIgnoreCaseOrThrow(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    private User findByResetPasswordTokenOrThrow(String token) {
        return userRepository.findByResetPasswordToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Validation token is invalid or has expired."));
    }

    private User findByVerificationTokenOrThrow(String token) {
        return userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Verification token is invalid or has expired."));
    }

    private boolean isSameAsCurrentPassword(String providedPassword, String currentPasswordHash) {
        return passwordEncoder.matches(providedPassword, currentPasswordHash);
    }
}

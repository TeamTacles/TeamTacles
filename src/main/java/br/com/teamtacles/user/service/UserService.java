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
import br.com.teamtacles.user.validator.PasswordMatchValidator;
import org.springframework.transaction.annotation.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import br.com.teamtacles.user.validator.PasswordMatchValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;
    private final PasswordMatchValidator passwordMatchValidator;
    private final EmailService emailService;

    public UserService(UserRepository userRepository,PasswordEncoder passwordEncoder, ModelMapper modelMapper, RoleRepository roleRepository, PasswordMatchValidator passwordMatchValidator, EmailService emailService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.modelMapper = modelMapper;
        this.passwordMatchValidator = passwordMatchValidator;
        this.emailService = emailService;
    }

    @Transactional
    public UserResponseDTO createUser(UserRequestRegisterDTO userRequestRegisterDTO) {
        log.info("Attempting to create a new user with username: {}", userRequestRegisterDTO.getUsername());
        if(userRepository.existsByUsername(userRequestRegisterDTO.getUsername())){
            log.warn("User creation failed: Username {} already exists", userRequestRegisterDTO.getUsername());
            throw new UsernameAlreadyExistsException("Username/email already exists");
        }

        if(userRepository.existsByEmail(userRequestRegisterDTO.getEmail())){
            log.info("User creation failed: Email {} already exists", userRequestRegisterDTO.getEmail());

            throw new EmailAlreadyExistsException("Username/email already exists");
        }

        if(!userRequestRegisterDTO.getPassword().equals(userRequestRegisterDTO.getPasswordConfirm())){
            log.warn("User creation failed for username {}: password and confirmation do not match.", userRequestRegisterDTO.getUsername());
            throw new PasswordMismatchException("Password and confirmation don't match");
        }
        log.debug("User creation validations passed for username: {}. Proceeding to create user entity.", userRequestRegisterDTO.getUsername());


        User user = new User();
        user.setUsername(userRequestRegisterDTO.getUsername());
        user.setEmail(userRequestRegisterDTO.getEmail());
        user.setPassword(passwordEncoder.encode(userRequestRegisterDTO.getPassword()));
        Role userRole = roleRepository.findByRoleName(ERole.USER)
                .orElseThrow(() -> new ResourceNotFoundException("Error: Role USER not found."));
        user.setRoles(Set.of(userRole));
        String token = UUID.randomUUID().toString();
        user.setVerificationToken(token);
        user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(1));
        user.setEnabled(false);

        User savedUser = userRepository.save(user);
        emailService.sendVerificationEmail(savedUser.getEmail(), token);
        log.info("User created successfully with ID: {}", savedUser.getId());


        UserResponseDTO userResponseDTO = modelMapper.map(savedUser, UserResponseDTO.class);

        return userResponseDTO;
    }
    
    public UserResponseDTO getUserById(User user) {
        return modelMapper.map(user, UserResponseDTO.class);
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

    @Transactional
    public void deleteUser(User user){
        userRepository.delete(user);
    }

    @Transactional
    public void resetPassword(String token, String newPassword, String passwordConfirm) {
        passwordMatchValidator.validate(newPassword, passwordConfirm);

        User user = userRepository.findByResetPasswordToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Validation token is invalid or has expired."));

        if (user.getResetPasswordTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Validation token is invalid or has expired.");
        }

        if (isSameAsCurrentPassword(newPassword, user.getPassword())) {
            throw new SameAsCurrentPasswordException("The new password cannot be the same as the current one.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));

        // mata o token
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);

        userRepository.save(user);
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

            if (isSameAsCurrentPassword(newPassword, user.getPassword())) {
                throw new SameAsCurrentPasswordException("The new password cannot be the same as the current one.");
            }

            user.setPassword(passwordEncoder.encode(newPassword));
        }
    }

    private boolean isSameAsCurrentPassword(String providedPassword, String currentPasswordHash) {
        return passwordEncoder.matches(providedPassword, currentPasswordHash);
    }

    @Transactional
    public void verifyUser(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Verification token is invalid or has expired."));
        if (user.getVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("The verification token has expired.");
        }
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


}

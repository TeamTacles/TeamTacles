package br.com.teamtacles.user.service;

import br.com.teamtacles.common.exception.*;
import br.com.teamtacles.config.aop.BusinessActivityLog;
import br.com.teamtacles.infrastructure.email.EmailService;
import br.com.teamtacles.project.service.ProjectService;
import br.com.teamtacles.task.service.TaskService;
import br.com.teamtacles.team.service.TeamService;
import br.com.teamtacles.user.dto.request.UserRequestRegisterDTO;
import br.com.teamtacles.user.dto.request.UserRequestUpdateDTO;
import br.com.teamtacles.user.dto.response.UserResponseDTO;
import br.com.teamtacles.user.enumeration.ERole;
import br.com.teamtacles.user.model.Role;
import br.com.teamtacles.user.model.User;
import br.com.teamtacles.user.repository.RoleRepository;
import br.com.teamtacles.user.repository.UserRepository;
import br.com.teamtacles.user.validator.*;
import org.springframework.transaction.annotation.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
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
    private final PasswordUpdateValidator passwordUpdateValidator;

    private final EmailService emailService;

    private final ModelMapper modelMapper;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       ModelMapper modelMapper, RoleRepository roleRepository,
                       PasswordMatchValidator passwordMatchValidator, EmailService emailService,
                       UserUniquenessValidator userUniquenessValidator,
                       UserTokenValidator userTokenValidator,
                       NewPasswordValidator newPasswordValidator,
                       PasswordUpdateValidator passwordUpdateValidator) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.modelMapper = modelMapper;
        this.passwordMatchValidator = passwordMatchValidator;
        this.emailService = emailService;
        this.userUniquenessValidator = userUniquenessValidator;
        this.userTokenValidator = userTokenValidator;
        this.newPasswordValidator = newPasswordValidator;
        this.passwordUpdateValidator = passwordUpdateValidator;
    }

    @BusinessActivityLog(action = "Create User Account")
    @Transactional
    public UserResponseDTO createUser(UserRequestRegisterDTO registerDTO) {
        userUniquenessValidator.validate(registerDTO);
        passwordMatchValidator.validate(registerDTO.getPassword(), registerDTO.getPasswordConfirm());

        Role userRole = roleRepository.findByRoleName(ERole.USER)
                .orElseThrow(() -> new ResourceNotFoundException("Error: Role USER not found."));

        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setEmail(registerDTO.getEmail());
        user.definePassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.addRole(userRole);

        String token = UUID.randomUUID().toString();
        user.assignVerificationToken(token, OffsetDateTime.now().plusHours(1));

        User savedUser = userRepository.save(user);
        emailService.sendVerificationEmail(savedUser.getEmail(), token);

        return modelMapper.map(savedUser, UserResponseDTO.class);
    }

    @BusinessActivityLog(action = "Update User Profile")
    @Transactional
    public UserResponseDTO updateUser(UserRequestUpdateDTO userRequestDTO, User user) {
        userUniquenessValidator.validate(userRequestDTO, user.getId());
        passwordUpdateValidator.validate(userRequestDTO, user);

        if (userRequestDTO.getUsername() != null && !userRequestDTO.getUsername().isBlank()) {
            user.setUsername(userRequestDTO.getUsername());
        }

        if (userRequestDTO.getEmail() != null && !userRequestDTO.getEmail().isBlank()) {
            user.setEmail(userRequestDTO.getEmail());
        }

        if (userRequestDTO.getPassword() != null && !userRequestDTO.getPassword().isBlank()) {
            user.updatePassword(passwordEncoder.encode(userRequestDTO.getPassword()));
        }

        User updatedUser = userRepository.save(user);
        return modelMapper.map(updatedUser, UserResponseDTO.class);
    }

    @BusinessActivityLog(action = "Reset User Password")
    @Transactional
    public void resetPassword(String token, String newPassword, String passwordConfirm) {
        passwordMatchValidator.validate(newPassword, passwordConfirm);

        User user = findByResetPasswordTokenOrThrow(token);

        userTokenValidator.validatePasswordResetToken(user);
        newPasswordValidator.validate(newPassword, user.getPassword());

        user.updatePassword(passwordEncoder.encode(newPassword));

        userRepository.save(user);
    }

    @BusinessActivityLog(action = "Verify User Account")
    @Transactional
    public void verifyUser(String token) {
        User user = findByVerificationTokenOrThrow(token);
        userTokenValidator.validateVerificationToken(user);

        user.confirmAccountVerification();

        userRepository.save(user);
    }

    @BusinessActivityLog(action = "Resend Verification Email")
    @Transactional
    public void resendVerificationEmail(String email) {
        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            if (!user.isEnabled()) {

                String token = UUID.randomUUID().toString();
                user.assignVerificationToken(token, OffsetDateTime.now().plusHours(24));

                userRepository.save(user);
                emailService.sendVerificationEmail(user.getEmail(), token);
            }
        });
    }
    
    public UserResponseDTO getUserById(User user) {
        return modelMapper.map(user, UserResponseDTO.class);
    }

    @BusinessActivityLog(action = "Delete User Account")
    @Transactional
    public void deleteUser(User user) {
        userRepository.deleteById(user.getId());
    }

    public User findUserEntityById(Long userId) {
        return findUserByIdOrThrow(userId);
    }

    public User findUserEntityByEmail(String email) {
        return findByEmailIgnoreCaseOrThrow(email);
    }

    public List<User> findUsersByIdsOrThrow(Set<Long> usersIds) {
        List<User> users = userRepository.findAllById(usersIds);

        if (users.size() != usersIds.size()) {
            throw new ResourceNotFoundException("One or more users were not found.");
        }

        return users;
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

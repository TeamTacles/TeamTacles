package br.com.teamtacles.user.service;

import br.com.teamtacles.common.exception.*;
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


import java.time.LocalDateTime;
import java.util.Set;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;
    private final PasswordMatchValidator passwordMatchValidator;

    public UserService(UserRepository userRepository,PasswordEncoder passwordEncoder, ModelMapper modelMapper, RoleRepository roleRepository, PasswordMatchValidator passwordMatchValidator){
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.modelMapper = modelMapper;
        this.passwordMatchValidator = passwordMatchValidator;
    }

    @Transactional
    public UserResponseDTO createUser(UserRequestRegisterDTO userRequestRegisterDTO) {
        if(userRepository.existsByUsername(userRequestRegisterDTO.getUsername())){
            throw new UsernameAlreadyExistsException("Username/email already exists");
        }

        if(userRepository.existsByEmail(userRequestRegisterDTO.getEmail())){
            throw new EmailAlreadyExistsException("Username/email already exists");
        }

        if(!userRequestRegisterDTO.getPassword().equals(userRequestRegisterDTO.getPasswordConfirm())){
            throw new PasswordMismatchException("Password and confirmation don't match");
        }

        User user = new User();
        user.setUsername(userRequestRegisterDTO.getUsername());
        user.setEmail(userRequestRegisterDTO.getEmail());
        user.setPassword(passwordEncoder.encode(userRequestRegisterDTO.getPassword()));
        Role userRole = roleRepository.findByRoleName(ERole.USER)
                .orElseThrow(() -> new ResourceNotFoundException("Error: Role USER not found."));
        user.setRoles(Set.of(userRole));
        User savedUser = userRepository.save(user);

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

    @Transactional
    public void deleteUser(User user){
        userRepository.delete(user);
    }

    private boolean isSameAsCurrentPassword(String providedPassword, String currentPasswordHash) {
        return passwordEncoder.matches(providedPassword, currentPasswordHash);
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
}

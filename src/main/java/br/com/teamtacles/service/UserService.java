package br.com.teamtacles.service;

import br.com.teamtacles.dto.request.user.UserRequestRegisterDTO;
import br.com.teamtacles.dto.request.user.UserRequestUpdateDTO;
import br.com.teamtacles.dto.response.user.UserResponseDTO;
import br.com.teamtacles.enumeration.ERole;
import br.com.teamtacles.exception.*;
import br.com.teamtacles.model.Role;
import br.com.teamtacles.model.User;
import br.com.teamtacles.repository.RoleRepository;
import br.com.teamtacles.repository.UserRepository;
import br.com.teamtacles.validator.PasswordMatchValidator;
import org.apache.coyote.BadRequestException;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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

    public UserResponseDTO updateUser(UserRequestUpdateDTO userRequestDTO, User user) {
        modelMapper.map(userRequestDTO, user);

        if (userRequestDTO.getPassword() != null && !userRequestDTO.getPassword().isBlank()) {
            passwordMatchValidator.validate(userRequestDTO.getPassword(), userRequestDTO.getPasswordConfirm());

            if (isSameAsCurrentPassword(userRequestDTO.getPassword(), user.getPassword())) {
                throw new SameAsCurrentPasswordException("The new password cannot be the same as the current one.");
            }

            user.setPassword(passwordEncoder.encode(userRequestDTO.getPassword()));
        }

        User updatedUser = userRepository.save(user);
        return modelMapper.map(updatedUser, UserResponseDTO.class);
    }

    public void deleteUser(User user){
        userRepository.delete(user);
    }

    private boolean isSameAsCurrentPassword(String providedPassword, String currentPasswordHash) {
        return passwordEncoder.matches(providedPassword, currentPasswordHash);
    }
}

package br.com.teamtacles.service;

import br.com.teamtacles.dto.request.user.UserRequestDTO;
import br.com.teamtacles.dto.response.user.UserResponseDTO;
import br.com.teamtacles.enumeration.ERole;
import br.com.teamtacles.exception.EmailAlreadyExistsException;
import br.com.teamtacles.exception.PasswordMismatchException;
import br.com.teamtacles.exception.ResourceNotFoundException;
import br.com.teamtacles.exception.UsernameAlreadyExistsException;
import br.com.teamtacles.model.Role;
import br.com.teamtacles.model.User;
import br.com.teamtacles.repository.RoleRepository;
import br.com.teamtacles.repository.UserRepository;
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

    public UserService(UserRepository userRepository,PasswordEncoder passwordEncoder, ModelMapper modelMapper, RoleRepository roleRepository){
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.modelMapper = modelMapper;
    }

    public UserResponseDTO createUser(UserRequestDTO userRequestDTO) {
        if(userRepository.existsByUsername(userRequestDTO.getUsername())){
            throw new UsernameAlreadyExistsException("Username/email already exists");
        }

        if(userRepository.existsByEmail(userRequestDTO.getEmail())){
            throw new EmailAlreadyExistsException("Username/email already exists");
        }

        if(!userRequestDTO.getPassword().equals(userRequestDTO.getPasswordConfirm())){
            throw new PasswordMismatchException("Password and confirmation don't match");
        }

        User user = new User();
        user.setUsername(userRequestDTO.getUsername());
        user.setEmail(userRequestDTO.getEmail());
        user.setPassword(passwordEncoder.encode(userRequestDTO.getPassword()));
        Role userRole = roleRepository.findByRoleName(ERole.USER)
                .orElseThrow(() -> new ResourceNotFoundException("Error: Role USER not found."));
        user.setRoles(Set.of(userRole));
        User savedUser = userRepository.save(user);

        UserResponseDTO userResponseDTO = modelMapper.map(savedUser, UserResponseDTO.class);

        return userResponseDTO;
    }
}

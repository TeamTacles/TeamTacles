package br.com.teamtacles.user.validator;

import br.com.teamtacles.common.exception.EmailAlreadyExistsException;
import br.com.teamtacles.common.exception.UsernameAlreadyExistsException;
import br.com.teamtacles.user.dto.request.UserRequestRegisterDTO;
import br.com.teamtacles.user.dto.request.UserRequestUpdateDTO;
import br.com.teamtacles.user.model.User;
import br.com.teamtacles.user.repository.UserRepository;
import org.springframework.stereotype.Component;

@Component
public class UserUniquenessValidator {

    private final UserRepository userRepository;

    public UserUniquenessValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void validate(UserRequestRegisterDTO dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new UsernameAlreadyExistsException("Username '" + dto.getUsername() + "' already exists.");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new EmailAlreadyExistsException("Email '" + dto.getEmail() + "' already exists.");
        }
    }

    public void validate(UserRequestUpdateDTO dto, Long actingUserId) {
        if (userRepository.existsByUsernameAndIdNot(dto.getUsername(), actingUserId)) {
            throw new UsernameAlreadyExistsException("Username '" + dto.getUsername() + "' already exists.");
        }
        if (userRepository.existsByEmailAndIdNot(dto.getEmail(), actingUserId)) {
            throw new EmailAlreadyExistsException("Email '" + dto.getEmail() + "' already exists.");
        }
    }
}

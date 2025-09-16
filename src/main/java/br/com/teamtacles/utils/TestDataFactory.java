package br.com.teamtacles.utils;

import br.com.teamtacles.user.dto.request.UserRequestRegisterDTO;
import br.com.teamtacles.user.enumeration.ERole;
import br.com.teamtacles.user.model.Role;
import br.com.teamtacles.user.model.User;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

public class TestDataFactory {

    public static User createValidUser() {
        User user = new User();
        try {
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            ReflectionUtils.setField(idField, user, 1L);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }

        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("encodedPassword123");
        user.setEnabled(true);
        return user;
    }

    public static User createUnverifiedUser() {
        User user = createValidUser();
        user.setEnabled(false);
        user.setVerificationToken("valid-token-123");
        user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(1));
        return user;
    }

    public static Role createDefaultUserRole() {
        return new Role(1L, ERole.USER);
    }

    public static UserRequestRegisterDTO createValidUserRequestRegisterDTO() {
        return new UserRequestRegisterDTO(
                "testuser",
                "test@gmail.com",
                "Password123",
                "Password123"
        );
    }
}

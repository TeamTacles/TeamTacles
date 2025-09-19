package br.com.teamtacles.task.validator;

import br.com.teamtacles.task.dto.request.TaskAssignmentRequestDTO;
import br.com.teamtacles.task.enumeration.ETaskRole;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class TaskAssignmentRoleValidator {
    public void validate(Set<TaskAssignmentRequestDTO> assignmentsDTO) {
        boolean hasOwnerRole = assignmentsDTO.stream()
                .anyMatch(dto -> dto.getRole() == ETaskRole.OWNER);

        if (hasOwnerRole) {
            throw new IllegalArgumentException("The OWNER role cannot be assigned through this method. It is set automatically on task creation.");
        }
    }
}

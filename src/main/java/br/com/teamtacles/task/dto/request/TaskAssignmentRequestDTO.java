package br.com.teamtacles.task.dto.request;

import br.com.teamtacles.task.enumeration.ETaskRole;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskAssignmentRequestDTO {

    @NotNull(message = "User ID cannot be null.")
    private Long userId;

    @NotNull(message = "Task role cannot be null.")
    private ETaskRole taskRole;
}

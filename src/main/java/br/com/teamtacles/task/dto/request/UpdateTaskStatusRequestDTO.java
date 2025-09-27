package br.com.teamtacles.task.dto.request;

import br.com.teamtacles.task.enumeration.ETaskStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTaskStatusRequestDTO {

    @NotNull(message = "The new status cannot be null.")
    private ETaskStatus newStatus;

    @Size(max = 300, message = "Completion comment cannot exceed 300 characters.")
    private String completionComment;
}

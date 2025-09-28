package br.com.teamtacles.task.dto.response;

import br.com.teamtacles.task.enumeration.ETaskRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "UserAssignmentResponse", description = "DTO representing a user's assignment to a task.")
public class UserAssignmentResponseDTO {

    @Schema(description = "The unique identifier of the user.", example = "101")
    private Long userId;

    @Schema(description = "The username of the assigned user.", example = "john.doe")
    private String username;

    @Schema(description = "The role of the user in the context of this task.", example = "ASSIGNEE")
    private ETaskRole taskRole;
}
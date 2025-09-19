package br.com.teamtacles.task.dto.response;

import br.com.teamtacles.task.enumeration.ETaskRole;
import lombok.Data;

@Data
public class UserAssignmentResponseDTO {
    private Long userId;
    private String username;
    private ETaskRole taskRole;

}
package br.com.teamtacles.task.dto.response;

import br.com.teamtacles.task.enumeration.ETaskStatus;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.Set;

@Data
public class TaskResponseDTO {
    private Long id;
    private String title;
    private String description;
    private ETaskStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime dueDate;
    private Long projectId;
    private Long ownerId;
    private Set<UserAssignmentResponseDTO> assignments;
}

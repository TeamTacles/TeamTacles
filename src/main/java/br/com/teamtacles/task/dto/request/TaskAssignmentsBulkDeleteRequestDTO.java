package br.com.teamtacles.task.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class TaskAssignmentsBulkDeleteRequestDTO {

    @NotEmpty(message = "The list of user IDs cannot be empty")
    private Set<Long> userIds;
}

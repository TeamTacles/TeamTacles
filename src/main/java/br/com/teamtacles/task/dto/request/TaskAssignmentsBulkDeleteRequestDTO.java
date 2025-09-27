package br.com.teamtacles.task.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskAssignmentsBulkDeleteRequestDTO {

    @NotEmpty(message = "The list of user IDs cannot be empty")
    private Set<Long> userIds;
}

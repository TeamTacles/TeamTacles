package br.com.teamtacles.task.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@Schema(name = "TaskSummaryReport", description = "DTO that provides a summary count of tasks by status.")
public class TaskSummaryDTO {

    @Schema(description = "The total number of tasks in the project.", example = "50")
    private final long totalCount;

    @Schema(description = "The number of tasks with 'DONE' status.", example = "25")
    private final long doneCount;

    @Schema(description = "The number of tasks with 'IN_PROGRESS' status.", example = "10")
    private final long inProgressCount;

    @Schema(description = "The number of tasks with 'TO_DO' status.", example = "15")
    private final long toDoCount;

    @Schema(description = "The number of tasks that are past their due date.", example = "5")
    private final long overdueCount;
}
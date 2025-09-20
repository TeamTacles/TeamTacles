package br.com.teamtacles.project.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TaskSummary {
    private final long doneCount;
    private final long inProgressCount;
    private final long toDoCount;
    private final long overdueCount;
}
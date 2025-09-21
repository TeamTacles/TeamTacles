
package br.com.teamtacles.project.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DashboardSummaryDTO {
    private final long totalCount;
    private final long doneCount;
    private final long inProgressCount;
    private final long toDoCount;
    private final long overdueCount;
}
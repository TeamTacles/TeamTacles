package br.com.teamtacles.project.dto.response;

import br.com.teamtacles.task.enumeration.ETaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.EnumMap;
import java.util.Map;

@Data
@NoArgsConstructor
@Schema(name = "MemberTaskDistribution", description = "DTO representing the distribution of tasks by status for a single project member based on applied filters.")
public class MemberTaskDistributionDTO {

    @Schema(description = "The unique identifier of the user.", example = "101")
    private Long userId;

    @Schema(description = "The username of the member.", example = "john.doe")
    private String username;

    @Schema(description = "The total number of tasks assigned to this member matching the filters.", example = "15")
    private long totalTasksCount;

    @Schema(description = "A map containing the count of tasks for each status (TO_DO, IN_PROGRESS, DONE, OVERDUE).")
    private Map<ETaskStatus, Long> statusCounts;

    public MemberTaskDistributionDTO(Long userId, String username) {
        this.userId = userId;
        this.username = username;
        this.totalTasksCount = 0;
        this.statusCounts = new EnumMap<>(ETaskStatus.class);
        for (ETaskStatus status : ETaskStatus.values()) {
            this.statusCounts.put(status, 0L);
        }
    }

    public void incrementTaskStatus(ETaskStatus status) {
        this.totalTasksCount++;
        this.statusCounts.compute(status, (key, count) -> (count == null ? 0 : count) + 1);
    }
}
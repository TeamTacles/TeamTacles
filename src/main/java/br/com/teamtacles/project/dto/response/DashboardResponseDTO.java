package br.com.teamtacles.project.dto.response;

import br.com.teamtacles.task.dto.response.TaskSummaryDTO;
import br.com.teamtacles.task.dto.response.TaskResponseDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ProjectDashboardResponse", description = "DTO for the project dashboard, showing a summary, recent tasks, and member performance.")
public class DashboardResponseDTO {

    @Schema(description = "A summary of task statuses within the project.")
    private TaskSummaryDTO summary;

    @Schema(description = "A list of the most recently updated tasks in the project.")
    private List<TaskResponseDTO> recentsTasks;

    @Schema(description = "A ranked list of members by their performance (e.g., completed tasks).")
    private List<MemberPerformanceDTO> memberPerformanceRanking;

}

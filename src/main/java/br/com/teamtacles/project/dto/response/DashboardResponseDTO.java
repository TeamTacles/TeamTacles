package br.com.teamtacles.project.dto.response;

import br.com.teamtacles.task.dto.response.TaskSummaryDTO;
import br.com.teamtacles.task.dto.response.TaskResponseDTO;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponseDTO {

    private TaskSummaryDTO summary;
    private List<TaskResponseDTO> recentsTasks;
    private List<MemberPerformanceDTO> memberPerformanceRanking;

}

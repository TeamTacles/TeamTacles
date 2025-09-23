package br.com.teamtacles.project.dto.response;

import br.com.teamtacles.task.dto.response.TaskSummaryDTO;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ProjectReportDTO {
    private TaskSummaryDTO summary;
    private List<MemberPerformanceDTO> memberPerformanceRanking;
}
package br.com.teamtacles.project.dto.response;

import br.com.teamtacles.task.dto.response.TaskSummaryDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "ProjectReport", description = "DTO for a comprehensive project report.")
public class ProjectReportDTO {

    @Schema(description = "A summary of task statuses for the report.")
    private TaskSummaryDTO summary;

    @Schema(description = "A ranked list of member performance for the report.")
    private List<MemberPerformanceDTO> memberPerformanceRanking;
}
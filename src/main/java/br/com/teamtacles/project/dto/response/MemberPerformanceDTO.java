package br.com.teamtacles.project.dto.response;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "MemberPerformance", description = "DTO representing the performance of a single project member.")
public class MemberPerformanceDTO {

    @Schema(description = "The unique identifier of the user.", example = "101")
    private Long userId;

    @Schema(description = "The username of the member.", example = "john.doe")
    private String username;

    @Schema(description = "The total number of tasks completed by this member.", example = "25")
    private long completedTasksCount;
}


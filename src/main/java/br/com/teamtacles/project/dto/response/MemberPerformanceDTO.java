package br.com.teamtacles.project.dto.response;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberPerformanceDTO {
    private Long userId;
    private String username;
    private long completedTasksCount;
}


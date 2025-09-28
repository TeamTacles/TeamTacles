package br.com.teamtacles.task.dto.request;

import br.com.teamtacles.task.enumeration.ETaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "TaskFilterReport", description = "DTO for filtering tasks for reports and listings.")
public class TaskFilterReportDTO {

    @Schema(description = "Filter tasks by title (partial match).", example = "Develop")
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    private String title;

    @Schema(description = "Filter tasks by their current status.", example = "IN_PROGRESS")
    private ETaskStatus status;

    @Schema(description = "Filter tasks assigned to a specific user by their ID.", example = "101")
    private Long assignedUserId;

    @Schema(description = "Filter for tasks updated after this date.", example = "2023-01-01")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate updatedAtAfter;

    @Schema(description = "Filter for tasks updated before this date.", example = "2023-12-31")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate updatedAtBefore;

    @Schema(description = "Filter for tasks with a due date after this date.", example = "2024-01-15")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueDateAfter;

    @Schema(description = "Filter for tasks with a due date before this date.", example = "2024-03-31")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueDateBefore;

    @Schema(description = "Filter for tasks created after this date.", example = "2023-01-01")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate createdAtAfter;

    @Schema(description = "Filter for tasks created before this date.", example = "2023-06-30")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate createdAtBefore;


    @Schema(description = "Filter for tasks concluded after this date.", example = "2024-01-01")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate conclusionDateAfter;

    @Schema(description = "Filter for tasks concluded before this date.", example = "2024-06-30")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate conclusionDateBefore;
}

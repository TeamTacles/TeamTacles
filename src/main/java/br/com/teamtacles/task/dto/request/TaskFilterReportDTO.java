package br.com.teamtacles.task.dto.request;

import br.com.teamtacles.task.enumeration.ETaskStatus;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskFilterReportDTO {

    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    private String title;

    private ETaskStatus status;
    private Long assignedUserId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate updatedAtAfter;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate updatedAtBefore;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueDateAfter;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueDateBefore;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate createdAtAfter;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate createdAtBefore;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate conclusionDateAfter;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate conclusionDateBefore;
}

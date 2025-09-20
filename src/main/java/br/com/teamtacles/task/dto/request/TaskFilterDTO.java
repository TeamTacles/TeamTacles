package br.com.teamtacles.task.dto.request;

import br.com.teamtacles.task.enumeration.ETaskStatus;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class TaskFilterDTO {

    private String title;
    private ETaskStatus status;

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

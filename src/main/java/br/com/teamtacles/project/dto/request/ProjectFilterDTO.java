package br.com.teamtacles.project.dto.request;

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
@Schema(name = "ProjectFilter", description = "DTO for filtering projects.")
public class ProjectFilterDTO {

    @Schema(description = "Filter by project title.", example = "New Marketing Campaign")
    @Size(max = 100, message = "Title cannot exceed 100 characters")
    private String title;

    @Schema(description = "Filter for projects created after this date.", example = "2023-01-01")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate createdAtAfter;

    @Schema(description = "Filter for projects created before this date.", example = "2023-12-31")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate createdAtBefore;
}

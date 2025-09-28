package br.com.teamtacles.team.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "TeamFilter", description = "DTO for filtering a list of teams")
public class TeamFilterDTO {

    @Schema(description = "Filter teams by name (partial match).", example = "Alpha")
    private String name;

    @Schema(description = "Filter for teams created on or after this date.", example = "2023-01-01")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate createdAtAfter;
    
    @Schema(description = "Filter for teams created on or before this date.", example = "2023-12-31")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate createdAtBefore;
}

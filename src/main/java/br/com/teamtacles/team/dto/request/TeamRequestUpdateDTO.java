package br.com.teamtacles.team.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamRequestUpdateDTO {

    @Size(min = 3, max = 50)
    private String name;

    @Size(max = 250)
    private String description;
}
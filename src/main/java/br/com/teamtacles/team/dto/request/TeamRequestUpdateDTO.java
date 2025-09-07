package br.com.teamtacles.team.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TeamRequestUpdateDTO {

    @NotBlank(message = "The team name cannot be blank")
    @Size(min = 3, max = 50)
    private String name;

    @Size(max = 250)
    private String description;
}
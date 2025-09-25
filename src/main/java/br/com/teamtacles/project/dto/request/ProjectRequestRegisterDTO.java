package br.com.teamtacles.project.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class ProjectRequestRegisterDTO {

    @NotBlank
    @Size(min=3, max=50)
    private String title;

    @Size(max=250)
    private String description;
}

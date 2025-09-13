package br.com.teamtacles.project.dto.request;


import br.com.teamtacles.project.enumeration.EProjectRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateMemberRoleProjectRequestDTO {

    @NotNull(message = "The new role cannot be null")
    private EProjectRole newRole;
}

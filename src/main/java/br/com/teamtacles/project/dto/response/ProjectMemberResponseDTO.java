package br.com.teamtacles.project.dto.response;

import br.com.teamtacles.project.enumeration.EProjectRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ProjectMemberResponse", description = "DTO representing a member of a project.")
public class ProjectMemberResponseDTO {

    @Schema(description = "The unique identifier of the user.", example = "101")
    private Long userId;

    @Schema(description = "The username of the member.", example = "jane.doe")
    private String username;

    @Schema(description = "The email address of the member.", example = "jane.doe@example.com")
    private String email;

    @Schema(description = "The member's role within the project.", example = "MEMBER")
    private EProjectRole projectRole;
}

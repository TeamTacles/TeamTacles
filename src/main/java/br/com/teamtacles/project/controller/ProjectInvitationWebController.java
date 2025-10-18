package br.com.teamtacles.project.controller;

import br.com.teamtacles.common.dto.response.MessageResponseDTO;
import br.com.teamtacles.common.exception.ErrorResponse;
import br.com.teamtacles.project.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@Tag(name = "Project Invitations (Web)", description = "Endpoint for accepting project invitations.")
public class ProjectInvitationWebController {

    private final ProjectService projectService;

    public ProjectInvitationWebController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @Operation(summary = "Accept email invitation", description = "Accepts a project invitation from an email link. This endpoint is designed to be opened in a browser.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Invitation accepted successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = MessageResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Invitation token is invalid or expired",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/accept-project-invitation-email")
    public String acceptProjectInvitation(@RequestParam("token") String token, Model model) {
        try {
            projectService.acceptInvitationFromEmail(token);
            return "invitation-accepted";

        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "invitation-accepted";
        }
    }
}
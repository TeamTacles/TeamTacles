package br.com.teamtacles.team.controller;

import br.com.teamtacles.common.dto.response.MessageResponseDTO;
import br.com.teamtacles.common.exception.ErrorResponse;
import br.com.teamtacles.team.service.TeamService;
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
@Tag(name = "Team Invitations (Web)", description = "Endpoint for accepting team invitations.")
public class TeamInvitationWebController {

    private final TeamService teamService;

    public TeamInvitationWebController(TeamService teamService) {
        this.teamService = teamService;
    }

    @Operation(summary = "Accept a team invitation from email", description = "Confirms an email-based invitation. This endpoint is typically called when a user clicks the link in their email.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Invitation accepted successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = MessageResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Invitation token is invalid or expired",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/accept-team-invitation-email")
    public String acceptTeamInvitation(@RequestParam("token") String token, Model model) {
        try {
            teamService.acceptInvitationFromEmail(token);
            return "invitation-accepted";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "invitation-accepted";
        }
    }
}
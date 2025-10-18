package br.com.teamtacles.team.controller;

import br.com.teamtacles.common.dto.response.InviteLinkResponseDTO;
import br.com.teamtacles.common.dto.response.MessageResponseDTO;
import br.com.teamtacles.common.dto.response.page.PagedResponse;
import br.com.teamtacles.common.exception.ErrorResponse;
import br.com.teamtacles.security.UserAuthenticated;
import br.com.teamtacles.team.dto.request.*;
import br.com.teamtacles.team.dto.response.TeamMemberResponseDTO;
import br.com.teamtacles.team.dto.response.TeamResponseDTO;
import br.com.teamtacles.team.dto.response.UserTeamResponseDTO;
import br.com.teamtacles.team.service.TeamService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/team")
@Tag(name = "Team Management", description = "Endpoints for creating, managing, and interacting with teams.")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @Operation(summary = "Create a new team", description = "Creates a new team and sets the authenticated user as the owner.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Team created successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = TeamResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "A team with this name already exists for the user",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<TeamResponseDTO> createTeam(
            @RequestBody @Valid TeamRequestRegisterDTO dto,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        TeamResponseDTO createdTeam = teamService.createTeam(dto, authenticatedUser.getUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTeam);
    }

    @Operation(summary = "Invite a user to a team via email", description = "Sends an email invitation to a user to join a team. Requires ADMIN or OWNER role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Invitation email sent successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden, user lacks permission to invite members",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Team or user to be invited not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Conflict, the user is already a member of this team",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{teamId}/invite-email")
    public ResponseEntity<Void> inviteMember(
            @PathVariable Long teamId,
            @RequestBody @Valid InvitedMemberRequestDTO dto,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        teamService.inviteMemberByEmail(teamId, dto, authenticatedUser.getUser());
        return ResponseEntity.status(HttpStatus.OK).build();
    }


    @Operation(summary = "Generate a team invitation link", description = "Generates a shareable link to invite users to a team. Requires ADMIN or OWNER role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Invitation link generated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = InviteLinkResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden, user lacks permission to generate links",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Team not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{teamId}/invite-link")
    public ResponseEntity<InviteLinkResponseDTO> generateInvitedLink(
            @PathVariable Long teamId,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        InviteLinkResponseDTO inviteLinkDTO = teamService.generateInvitedLink(teamId, authenticatedUser.getUser());
        return ResponseEntity.ok(inviteLinkDTO);
    }

    @Operation(summary = "Join a team using an invitation link", description = "Allows the authenticated user to join a team using a valid invitation token from a link.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully joined the team",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = TeamMemberResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Invitation token is invalid or expired",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Conflict, the user is already a member of this team",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/join")
    public ResponseEntity<TeamMemberResponseDTO> joinTeamWithLink(
            @RequestParam String token,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        TeamMemberResponseDTO teamMemberDTO = teamService.acceptInvitationFromLink(token, authenticatedUser.getUser());
        return ResponseEntity.ok(teamMemberDTO);
    }

    @Operation(summary = "Get all teams for the current user", description = "Retrieves a paginated and filtered list of teams the authenticated user is a member of.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved teams"),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<PagedResponse<UserTeamResponseDTO>> getAllTeamsByUser(
            @ModelAttribute TeamFilterDTO filter,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser,
            Pageable pageable) {
        PagedResponse<UserTeamResponseDTO> teams = teamService.getAllTeamsByUser(pageable, filter, authenticatedUser.getUser());
        return ResponseEntity.ok(teams);
    }

    @Operation(summary = "Get a team by ID", description = "Retrieves detailed information for a specific team, provided the user is a member.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved team data",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = TeamResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden, user is not a member of this team",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Team not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{teamId}")
    public ResponseEntity<TeamResponseDTO> getTeamById(
            @PathVariable Long teamId,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        TeamResponseDTO team = teamService.getTeamById(teamId, authenticatedUser.getUser());
        return ResponseEntity.ok(team);
    }

    @Operation(summary = "Get all members of a team", description = "Retrieves a paginated list of all members for a specific team. User must be a member of the team.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved members list"),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden, user is not a member of this team",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Team not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{teamId}/members")
    public ResponseEntity<PagedResponse<TeamMemberResponseDTO>> getAllMembersFromTeam(
            @PathVariable Long teamId,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser,
            Pageable pageable) {
        PagedResponse<TeamMemberResponseDTO> usersFromTeam = teamService.getAllMembersFromTeam(pageable, teamId, authenticatedUser.getUser());
        return ResponseEntity.ok(usersFromTeam);
    }

    @Operation(summary = "Update a team's details", description = "Updates the details of a team, such as its name. Requires OWNER role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Team updated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = TeamResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden, user is not the owner of this team",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Team not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{teamId}")
    public ResponseEntity<TeamResponseDTO> updateTeam(
            @PathVariable Long teamId,
            @RequestBody @Valid TeamRequestUpdateDTO dto,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        TeamResponseDTO updatedTeam = teamService.updateTeam(teamId, dto, authenticatedUser.getUser());
        return ResponseEntity.ok(updatedTeam);
    }

    @Operation(summary = "Update a member's role", description = "Updates the role of a specific member in a team. Requires ADMIN or OWNER role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Member role updated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = TeamMemberResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden, user lacks permission to update roles",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Team or member not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{teamId}/member/{userId}/role")
    public ResponseEntity<TeamMemberResponseDTO> updateMemberRole(
            @PathVariable Long teamId,
            @PathVariable Long userId,
            @RequestBody @Valid UpdateMemberRoleTeamRequestDTO dto,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        TeamMemberResponseDTO updatedMember = teamService.updateMemberRole(teamId, userId, dto, authenticatedUser.getUser());
        return ResponseEntity.ok(updatedMember);
    }

    @Operation(summary = "Delete a team", description = "Permanently deletes a team and all associated memberships. Requires OWNER role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Team deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden, user is not the owner of this team",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Team not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{teamId}")
    public ResponseEntity<Void> deleteTeam(
            @PathVariable Long teamId,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        teamService.deleteTeam(teamId, authenticatedUser.getUser());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Remove a member from a team", description = "Removes a user's membership from a team. Requires ADMIN or OWNER role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Member removed successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden, user lacks permission to remove members",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Team or membership not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{teamId}/member/{userId}")
    public ResponseEntity<Void> deleteMembershipFromTeam(
            @PathVariable Long teamId,
            @PathVariable Long userId,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        teamService.deleteMembershipFromTeam(teamId, userId, authenticatedUser.getUser());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Leave a team", description = "Allows an authenticated user to leave a team they are a member of.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Successfully left the team"),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden, the user is the owner and cannot leave the team",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Team or membership not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{teamId}/leave")
    public ResponseEntity<Void> leaveTeam(
            @PathVariable Long teamId,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        teamService.leaveTeam(teamId, authenticatedUser.getUser());
        return ResponseEntity.noContent().build();
    }
}

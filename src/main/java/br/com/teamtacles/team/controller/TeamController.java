package br.com.teamtacles.team.controller;

import br.com.teamtacles.security.UserAuthenticated;
import br.com.teamtacles.team.dto.request.TeamRequestRegisterDTO;
import br.com.teamtacles.team.dto.request.TeamRequestUpdateDTO;
import br.com.teamtacles.team.dto.request.UpdateMemberRoleRequestDTO;
import br.com.teamtacles.common.dto.page.PagedResponse;
import br.com.teamtacles.team.dto.response.TeamMemberResponseDTO;
import br.com.teamtacles.team.dto.response.TeamResponseDTO;
import br.com.teamtacles.team.dto.response.UserTeamResponseDTO;

import br.com.teamtacles.team.service.TeamService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import br.com.teamtacles.team.dto.request.InvitedMemberRequestDTO;

@RestController
@RequestMapping("/api/team")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @PostMapping
    public ResponseEntity<TeamResponseDTO> createTeam(
            @RequestBody @Valid TeamRequestRegisterDTO dto,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        TeamResponseDTO createdTeam = teamService.createTeam(dto, authenticatedUser.getUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTeam);
    }

    @GetMapping
    public ResponseEntity<PagedResponse<UserTeamResponseDTO>> getAllTeamsByUser(
            @AuthenticationPrincipal UserAuthenticated authenticatedUser,
            Pageable pageable) {
        PagedResponse<UserTeamResponseDTO> teams = teamService.getAllTeamsByUser(pageable, authenticatedUser.getUser());
        return ResponseEntity.ok(teams);
    }

    @GetMapping("/{teamId}")
    public ResponseEntity<TeamResponseDTO> getTeamById(
            @PathVariable Long teamId,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        TeamResponseDTO team = teamService.getTeamById(teamId, authenticatedUser.getUser());
        return ResponseEntity.ok(team);
    }

    @PatchMapping("/{teamId}")
    public ResponseEntity<TeamResponseDTO> updateTeam(
            @PathVariable Long teamId,
            @RequestBody @Valid TeamRequestUpdateDTO dto,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        TeamResponseDTO updatedTeam = teamService.updateTeam(teamId, dto, authenticatedUser.getUser());
        return ResponseEntity.ok(updatedTeam);
    }

    @PatchMapping("/{teamId}/member/{userId}/role")
    public ResponseEntity<TeamMemberResponseDTO> updateMemberRole(
            @PathVariable Long teamId,
            @PathVariable Long userId,
            @RequestBody @Valid UpdateMemberRoleRequestDTO dto,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        TeamMemberResponseDTO updatedMember = teamService.updateMemberRole(teamId, userId, dto, authenticatedUser.getUser());
        return ResponseEntity.ok(updatedMember);
    }

    @DeleteMapping("/{teamId}")
    public ResponseEntity<Void> deleteTeam(
            @PathVariable Long teamId,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        teamService.deleteTeam(teamId, authenticatedUser.getUser());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{teamId}/invite")
    public ResponseEntity<Void> inviteMember(
            @PathVariable Long teamId,
            @RequestBody @Valid InvitedMemberRequestDTO dto,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        teamService.inviteMember(teamId, dto, authenticatedUser.getUser());
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("/accept-invite")
    public ResponseEntity<String> acceptInvitation(@RequestParam String token) {
        teamService.acceptInvitation(token);
        return ResponseEntity.ok("Invitation accepted successfully.");
    }
}

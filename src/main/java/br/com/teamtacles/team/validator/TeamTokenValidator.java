package br.com.teamtacles.team.validator;

import br.com.teamtacles.common.exception.ResourceNotFoundException;
import br.com.teamtacles.team.model.Team;
import br.com.teamtacles.team.model.TeamMember;
import br.com.teamtacles.user.model.User;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Component
public class TeamTokenValidator {

    public void validateInvitationLinkToken(Team team) {
        if (team.getInvitationToken() == null || team.getInvitationTokenExpiry().isBefore(OffsetDateTime.now())) {
            throw new ResourceNotFoundException("Invitation token has expired.");
        }
    }

    public void validateInvitationToken(TeamMember teamMember) {
        if (teamMember.getInvitationToken() == null || teamMember.getInvitationTokenExpiry().isBefore(OffsetDateTime.now())) {
            throw new ResourceNotFoundException("Invitation token has expired.");
        }
    }
}

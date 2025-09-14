package br.com.teamtacles.team.validator;

import br.com.teamtacles.team.enumeration.ETeamRole;
import org.springframework.stereotype.Component;

@Component
public class TeamInvitationValidator {

    public void validateRole(ETeamRole role) {
        if(ETeamRole.OWNER.equals(role)) {
            throw new IllegalArgumentException("Cannot assign the OWNER role through an invitation.");
        }
    }
}

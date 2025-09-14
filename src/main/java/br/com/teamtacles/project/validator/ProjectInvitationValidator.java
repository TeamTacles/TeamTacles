package br.com.teamtacles.project.validator;

import br.com.teamtacles.project.enumeration.EProjectRole;
import br.com.teamtacles.team.enumeration.ETeamRole;
import org.springframework.stereotype.Component;

@Component
public class ProjectInvitationValidator {

    public void validateRole(EProjectRole role) {
        if(EProjectRole.OWNER.equals(role)) {
            throw new IllegalArgumentException("Cannot assign the OWNER role through an invitation.");
        }
    }
}

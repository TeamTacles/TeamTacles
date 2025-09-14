package br.com.teamtacles.team.validator;

import br.com.teamtacles.team.enumeration.ETeamRole;
import br.com.teamtacles.team.model.TeamMember;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class TeamMembershipActionValidator {

    public void validateRoleUpdate(TeamMember actingMember, TeamMember memberToUpdate, ETeamRole newRole) {
        if (memberToUpdate.getTeamRole().equals(ETeamRole.OWNER)) {
            throw new AccessDeniedException("The team OWNER's role cannot be changed.");
        }

        if (memberToUpdate.getTeamRole().equals(ETeamRole.ADMIN) && !actingMember.getTeamRole().isOwner()) {
            throw new AccessDeniedException("Only the OWNER can change an ADMIN's role.");
        }

        if (newRole.equals(ETeamRole.OWNER)) {
            throw new IllegalArgumentException("Cannot promote a user to OWNER.");
        }
    }

    public void validateDeletion(TeamMember actingMember, TeamMember memberToDelete) {
        boolean isActingUserOwner = actingMember.getTeamRole().isOwner();

        if (isActingUserOwner && actingMember.equals(memberToDelete)) {
            throw new AccessDeniedException("OWNER cannot remove themselves.");
        }

        if (!isActingUserOwner && memberToDelete.getTeamRole().isPrivileged()) {
            throw new AccessDeniedException("You cannot remove a member with role OWNER or ADMIN.");
        }
    }
}
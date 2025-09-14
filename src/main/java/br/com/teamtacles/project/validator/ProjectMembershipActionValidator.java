package br.com.teamtacles.project.validator;

import br.com.teamtacles.project.enumeration.EProjectRole;
import br.com.teamtacles.project.model.ProjectMember;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class ProjectMembershipActionValidator {

    public void validateRoleUpdate(ProjectMember actingMember, ProjectMember memberToUpdate, EProjectRole newRole) {
        if (memberToUpdate.getProjectRole().equals(EProjectRole.OWNER)) {
            throw new AccessDeniedException("The project OWNER's role cannot be changed.");
        }

        boolean isActingUserOwner = actingMember.getProjectRole().equals(EProjectRole.OWNER);

        if (memberToUpdate.getProjectRole().equals(EProjectRole.ADMIN) && !isActingUserOwner) {
            throw new AccessDeniedException("Only the project OWNER can change an ADMIN's role.");
        }

        if (EProjectRole.OWNER.equals(newRole)) {
            throw new IllegalArgumentException("Cannot promote a user to the OWNER role.");
        }
    }

    public void validateDeletion(ProjectMember actingMember, ProjectMember memberToDelete) {
        boolean isActingUserOwner = actingMember.getProjectRole().equals(EProjectRole.OWNER);

        if (isActingUserOwner && actingMember.equals(memberToDelete)) {
            throw new AccessDeniedException("OWNER cannot remove themselves from the project.");
        }

        if (!isActingUserOwner && memberToDelete.getProjectRole().isPrivileged()) {
            throw new AccessDeniedException("You cannot remove a member with role OWNER or ADMIN.");
        }
    }
}
package br.com.teamtacles.project.validator;

import br.com.teamtacles.common.exception.ResourceNotFoundException;
import br.com.teamtacles.project.model.Project;
import org.springframework.stereotype.Component;
import br.com.teamtacles.project.model.ProjectMember;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Component
public class ProjectTokenValidator {

    public void validateInvitationLinkToken(Project project) {
        if (project.getInvitationToken() == null || project.getInvitationTokenExpiry().isBefore(OffsetDateTime.now())) {
            throw new ResourceNotFoundException("Invitation token has expired.");
        }
    }

    public void validateInvitationEmailToken(ProjectMember projectMember) {
        if (projectMember.getInvitationToken() == null || projectMember.getInvitationTokenExpiry().isBefore(OffsetDateTime.now())) {
            throw new ResourceNotFoundException("Invitation token has expired.");
        }
    }
}

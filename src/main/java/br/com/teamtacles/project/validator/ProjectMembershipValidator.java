package br.com.teamtacles.project.validator;

import br.com.teamtacles.common.exception.ResourceAlreadyExistsException;
import br.com.teamtacles.project.model.Project;
import br.com.teamtacles.project.model.ProjectMember;
import br.com.teamtacles.project.repository.ProjectMemberRepository;
import br.com.teamtacles.user.model.User;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ProjectMembershipValidator {
    private final ProjectMemberRepository projectMemberRepository;

    public ProjectMembershipValidator(ProjectMemberRepository projectMemberRepository) {
        this.projectMemberRepository = projectMemberRepository;
    }

    public void validateNewMember(User user, Project project) {
        Optional<ProjectMember> existingMembership = projectMemberRepository.findByUserAndProject(user, project);

        if (existingMembership.isPresent() && existingMembership.get().isAcceptedInvite()) {
            throw new ResourceAlreadyExistsException("User is already a member of this project.");
        }
    }
}
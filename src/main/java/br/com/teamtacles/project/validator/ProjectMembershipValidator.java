package br.com.teamtacles.project.validator;

import br.com.teamtacles.common.exception.ResourceAlreadyExistsException;
import br.com.teamtacles.project.model.Project;
import br.com.teamtacles.project.repository.ProjectMemberRepository;
import br.com.teamtacles.team.model.Team;
import br.com.teamtacles.team.repository.TeamMemberRepository;
import br.com.teamtacles.user.model.User;
import org.springframework.stereotype.Component;

@Component
public class ProjectMembershipValidator {
    private final ProjectMemberRepository projectMemberRepository;

    public ProjectMembershipValidator(ProjectMemberRepository projectMemberRepository) {
        this.projectMemberRepository = projectMemberRepository;
    }

    public void validateNewMember(User user, Project project) {
        if (projectMemberRepository.findByUserAndProject(user, project).isPresent()) {
            throw new ResourceAlreadyExistsException("User is already a member of this project.");
        }
    }
}

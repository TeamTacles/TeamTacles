package br.com.teamtacles.project.validator;

import br.com.teamtacles.common.exception.ResourceAlreadyExistsException;
import br.com.teamtacles.project.dto.response.ProjectResponseDTO;
import br.com.teamtacles.project.repository.ProjectRepository;
import br.com.teamtacles.user.model.User;
import org.springframework.stereotype.Component;

@Component
public class ProjectTitleUniquenessValidator {

    private final ProjectRepository projectRepository;

    public ProjectTitleUniquenessValidator(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public void validate(String title, User owner) {
        if (projectRepository.existsByTitleIgnoreCaseAndOwner(title, owner)) {
            throw new ResourceAlreadyExistsException("Project name already in use by this creator.");
        }
    }
}

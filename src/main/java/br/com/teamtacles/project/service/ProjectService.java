package br.com.teamtacles.project.service;

import br.com.teamtacles.common.exception.ResourceAlreadyExistsException;
import br.com.teamtacles.project.dto.request.ProjectRequestRegisterDTO;
import br.com.teamtacles.project.dto.response.ProjectResponseDTO;
import br.com.teamtacles.project.enumeration.EProjectRole;
import br.com.teamtacles.project.model.Project;
import br.com.teamtacles.project.model.ProjectMember;
import br.com.teamtacles.project.repository.ProjectMemberRepository;
import br.com.teamtacles.project.repository.ProjectRepository;
import br.com.teamtacles.user.model.User;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ModelMapper modelMapper;

    public ProjectService(ProjectRepository projectRepository, ProjectMemberRepository projectMemberRepository, ModelMapper modelMapper) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.modelMapper = modelMapper;
    }

    @Transactional
    public ProjectResponseDTO createProject(ProjectRequestRegisterDTO requestDTO, User actingUser) {
        validateProjectNameUniqueness(requestDTO.getTitle(), actingUser );

        Project newProject = modelMapper.map(requestDTO, Project.class);
        newProject.setOwner(actingUser);

        ProjectMember creatorMembership = new ProjectMember(actingUser, newProject, EProjectRole.OWNER);
        creatorMembership.setAcceptedInvite(true);
        newProject.getMembers().add(creatorMembership);

        Project savedProject = projectRepository.save(newProject);
        return modelMapper.map(savedProject, ProjectResponseDTO.class);
    }

    private void validateProjectNameUniqueness(String title, User owner){
        if(projectRepository.existsByTitleIgnoreCaseAndOwner(title, owner)){
            throw new ResourceAlreadyExistsException("Project name already in use by this creator.");
        }
    }
}


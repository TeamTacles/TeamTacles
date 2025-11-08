package br.com.teamtacles.project.service;

import br.com.teamtacles.project.model.Project;
import br.com.teamtacles.project.repository.ProjectMemberRepository;
import br.com.teamtacles.user.model.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class ProjectAuthorizationService {

    private final ProjectMemberRepository projectMemberRepository;

    public ProjectAuthorizationService(ProjectMemberRepository projectMemberRepository) {
        this.projectMemberRepository = projectMemberRepository;
    }

    public boolean isMember(User user, Project project) {
        return projectMemberRepository.findByUserAndProject(user, project)
                .map(member -> member.isAcceptedInvite())
                .orElse(false);
    }

    public boolean isAdmin(User user, Project project) {
        return projectMemberRepository.findByUserAndProject(user, project)
                .map(member -> member.getProjectRole().isPrivileged() && member.isAcceptedInvite())
                .orElse(false);
    }

    public boolean isOwner(User user, Project project) {
        return project.getOwner().equals(user);
    }

    public void checkProjectMembership(User user, Project project) {
        if (!isMember(user, project)) {
            throw new AccessDeniedException("Access denied. You are not a member of this project.");
        }
    }
    public void checkProjectOwner(User user, Project project) {
        if (!isOwner(user, project)) {
            throw new AccessDeniedException("Permission denied. Action requires PROJECT OWNER role.");
        }
    }

    public void checkProjectAdmin(User user, Project project) {
        if (!isAdmin(user, project)) {
            throw new AccessDeniedException("Permission denied. Action requires ADMIN or OWNER role for this project.");
        }
    }
}

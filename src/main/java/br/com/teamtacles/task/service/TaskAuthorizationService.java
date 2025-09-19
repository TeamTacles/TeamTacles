package br.com.teamtacles.task.service;

import br.com.teamtacles.project.service.ProjectAuthorizationService;
import br.com.teamtacles.task.enumeration.ETaskRole;
import br.com.teamtacles.task.model.Task;
import br.com.teamtacles.user.model.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class TaskAuthorizationService {

    private final ProjectAuthorizationService projectAuthorizationService;

    public TaskAuthorizationService(ProjectAuthorizationService projectAuthorizationService) {
        this.projectAuthorizationService = projectAuthorizationService;
    }

    public void checkViewPermission(User user, Task task) {
        projectAuthorizationService.checkProjectMembership(user, task.getProject());
    }

    public void checkEditPermission(User user, Task task) {
        checkViewPermission(user, task);

        if (!isOwner(user, task) && !projectAuthorizationService.isAdmin(user, task.getProject())) {
            throw new AccessDeniedException("Permission denied. Only the task owner or a project admin or project owner can edit this task.");
        }
    }

    public void checkChangeStatusPermission(User user, Task task) {
        checkViewPermission(user, task);

        if (!isOwner(user, task) && !projectAuthorizationService.isAdmin(user, task.getProject()) && !isAssignee(user, task)) {
            throw new AccessDeniedException("Permission denied. Only the task owner or a project owner/admin or an assignee can change the status of this task.");
        }
    }

    public boolean isOwner(User user, Task task) {
        return task.getOwner().equals(user);
    }

    public boolean isAssignee(User user, Task task) {
        return task.getAssignments().stream()
                .anyMatch(assignment -> assignment.getUser().equals(user) &&
                        assignment.getTaskRole() == ETaskRole.ASSIGNEE);
    }
}

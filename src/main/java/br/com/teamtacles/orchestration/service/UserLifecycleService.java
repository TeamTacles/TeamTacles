package br.com.teamtacles.orchestration.service;

import br.com.teamtacles.config.aop.BusinessActivityLog;
import br.com.teamtacles.project.service.ProjectService;
import br.com.teamtacles.task.service.TaskService;
import br.com.teamtacles.team.service.TeamService;
import br.com.teamtacles.user.model.User;
import br.com.teamtacles.user.service.UserService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserLifecycleService {

    private final UserService userService;
    private final TeamService teamService;
    private final ProjectService projectService;
    private final TaskService taskService;

    public UserLifecycleService(UserService userService, TeamService teamService, ProjectService projectService, TaskService taskService) {
        this.userService = userService;
        this.teamService = teamService;
        this.projectService = projectService;
        this.taskService = taskService;
    }

    @BusinessActivityLog(action = "Handle User Deletion")
    @Transactional
    public void handleUserDeletion(User user) {
        taskService.handleOwnerDeletion(user);
        teamService.handleOwnerDeletion(user);
        projectService.handleOwnerDeletion(user);

        userService.deleteUser(user);
    }
}

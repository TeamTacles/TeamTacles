package br.com.teamtacles.orchestration.service;

import br.com.teamtacles.config.aop.BusinessActivityLog;
import br.com.teamtacles.project.service.ProjectService;
import br.com.teamtacles.task.service.TaskService;
import br.com.teamtacles.team.service.TeamService;
import br.com.teamtacles.user.model.User;
import br.com.teamtacles.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAccountService {

    private final UserService userService;
    private final TeamService teamService;
    private final ProjectService projectService;
    private final TaskService taskService;

    public UserAccountService(UserService userService, TeamService teamService, ProjectService projectService, TaskService taskService) {
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

    @BusinessActivityLog(action = "Leave Project and Tasks")
    @Transactional
    public void leaveProjectAndTasks(Long projectId, User user) {
        projectService.leaveProject(projectId, user);
        taskService.leaveAllTasks(projectId, user);
    }
}

package br.com.teamtacles.utils;

import br.com.teamtacles.project.dto.request.InviteProjectMemberRequestDTO;
import br.com.teamtacles.project.dto.request.ProjectRequestUpdateDTO;
import br.com.teamtacles.project.enumeration.EProjectRole;
import br.com.teamtacles.project.model.Project;
import br.com.teamtacles.project.model.ProjectMember;
import br.com.teamtacles.task.dto.request.TaskAssignmentRequestDTO;
import br.com.teamtacles.task.dto.request.TaskRequestRegisterDTO;
import br.com.teamtacles.task.dto.request.UpdateTaskStatusRequestDTO;
import br.com.teamtacles.task.dto.response.UserAssignmentResponseDTO;
import br.com.teamtacles.task.enumeration.ETaskRole;
import br.com.teamtacles.task.enumeration.ETaskStatus;
import br.com.teamtacles.task.model.Task;
import br.com.teamtacles.task.model.TaskAssignment;
import br.com.teamtacles.team.dto.request.InvitedMemberRequestDTO;
import br.com.teamtacles.team.dto.request.TeamRequestRegisterDTO;
import br.com.teamtacles.team.dto.request.TeamRequestUpdateDTO;
import br.com.teamtacles.team.dto.request.UpdateMemberRoleTeamRequestDTO;
import br.com.teamtacles.team.enumeration.ETeamRole;
import br.com.teamtacles.team.model.Team;
import br.com.teamtacles.team.model.TeamMember;
import br.com.teamtacles.user.dto.request.UserRequestRegisterDTO;
import br.com.teamtacles.user.enumeration.ERole;
import br.com.teamtacles.user.model.Role;
import br.com.teamtacles.user.model.User;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TestDataFactory {

    // ===================================================================================
    // DOMÍNIO: User
    // ===================================================================================

    public static User createValidUser() {
        return createUserWithId(1L, "testuser", "test@example.com");
    }

    public static User createUserWithId(Long id, String username, String email) {
        User user = new User();
        try {
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            ReflectionUtils.setField(idField, user, id);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }

        user.setUsername(username);
        user.setEmail(email);
        user.definePassword("encodedPassword123");
        user.confirmAccountVerification();
        return user;
    }

    public static User createUnverifiedUser() {
        User user = createValidUser();
        user.disableAccount();
        user.assignVerificationToken("valid-token-123", LocalDateTime.now().plusHours(1));
        return user;
    }

    public static Role createDefaultUserRole() {
        return new Role(1L, ERole.USER);
    }

    public static UserRequestRegisterDTO createValidUserRequestRegisterDTO() {
        return new UserRequestRegisterDTO("testuser", "test@gmail.com", "Password123", "Password123");
    }

    // ===================================================================================
    // DOMÍNIO: Team
    // ===================================================================================

    public static Team createTeam(User owner) {
        Team team = new Team("Team Tacles", "A great team", owner);
        try {
            Field idField = Team.class.getDeclaredField("id");
            idField.setAccessible(true);
            ReflectionUtils.setField(idField, team, 1L);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        TeamMember ownerMembership = new TeamMember(owner, team, ETeamRole.OWNER);
        ownerMembership.acceptedInvitation();
        try {
            Field memberIdField = TeamMember.class.getDeclaredField("id");
            memberIdField.setAccessible(true);
            ReflectionUtils.setField(memberIdField, ownerMembership, 999L); // ID fixo para o dono
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Failed to set ID on owner's TeamMember mock", e);
        }
        team.addMember(ownerMembership);
        return team;
    }

    public static TeamMember createTeamMember(User user, Team team, ETeamRole role) {
        TeamMember member = new TeamMember(user, team, role);
        try {
            Field idField = TeamMember.class.getDeclaredField("id");
            idField.setAccessible(true);
            ReflectionUtils.setField(idField, member, user.getId() + 100);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        member.acceptedInvitation();
        return member;
    }

    public static TeamRequestRegisterDTO createTeamRequestRegisterDTO() {
        return new TeamRequestRegisterDTO("New Awesome Team", "Description for the new team.");
    }

    public static InvitedMemberRequestDTO createInviteMemberRequestDTO(String email, ETeamRole role) {
        return new InvitedMemberRequestDTO(email, role);
    }

    public static UpdateMemberRoleTeamRequestDTO createUpdateMemberRoleTeamRequestDTO(ETeamRole newRole) {
        return new UpdateMemberRoleTeamRequestDTO(newRole);
    }

    public static TeamMember createTeamMemberWithExpiredInvitation(User user, Team team, ETeamRole role) {
        TeamMember member = new TeamMember(user, team, role);
        String token = member.generateInvitation();

        // simulando a expiração
        try {
            Field expiryField = TeamMember.class.getDeclaredField("invitationTokenExpiry");
            expiryField.setAccessible(true);
            ReflectionUtils.setField(expiryField, member, LocalDateTime.now().minusDays(1));
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Failed to set expiry date on TeamMember mock", e);
        }

        return member;
    }

    public static Team createTeamWithActiveInviteLink(User owner) {
        Team team = createTeam(owner);
        team.generateInviteLinkToken(); // Gera o token e a data de expiração
        return team;
    }

    public static TeamRequestUpdateDTO createTeamRequestUpdateDTO(String newName, String newDescription) {
        return new TeamRequestUpdateDTO(newName, newDescription);
    }

    // ===================================================================================
    // DOMÍNIO: Project
    // ===================================================================================

    public static Project createMockProject(User owner) {
        Project project = new Project("Mock Project", "A mock project for testing purposes.", owner);
        try {
            Field idField = Project.class.getDeclaredField("id");
            idField.setAccessible(true);
            ReflectionUtils.setField(idField, project, 100L);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Failed to set ID on Project mock", e);
        }

        ProjectMember ownerMembership = createProjectMember(owner, project, EProjectRole.OWNER, 999L);
        project.addMember(ownerMembership);
        return project;
    }

    public static ProjectMember createProjectMember(User user, Project project, EProjectRole role, Long mockId) {
        ProjectMember member = new ProjectMember(user, project, role);
        member.acceptedInvitation();

        try {
            Field idField = ProjectMember.class.getDeclaredField("id");
            idField.setAccessible(true);
            ReflectionUtils.setField(idField, member, mockId);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Failed to set ID on ProjectMember mock", e);
        }
        return member;
    }

    public static ProjectMember createPendingProjectMember(User user, Project project, EProjectRole role) {
        ProjectMember member = new ProjectMember(user, project, role);
        member.generateInvitation();

        try {
            Field idField = ProjectMember.class.getDeclaredField("id");
            idField.setAccessible(true);
            ReflectionUtils.setField(idField, member, user.getId() + 200);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Failed to set ID on ProjectMember mock", e);
        }
        return member;
    }

    public static ProjectRequestUpdateDTO createProjectRequestUpdateDTO() {
        return new ProjectRequestUpdateDTO("Updated Project Title", "Updated Description");
    }

    public static InviteProjectMemberRequestDTO createInviteProjectMemberRequestDTO(String email, EProjectRole role) {
        return new InviteProjectMemberRequestDTO(email, role);
    }

    // ===================================================================================
    // DOMÍNIO: Task
    // ===================================================================================

    public static Task createMockTask(Project project, User owner, OffsetDateTime dueDate) {
        Task task = new Task(project, "Mock Task", "A mock task for testing.", owner, dueDate);
        try {
            Field idField = Task.class.getDeclaredField("id");
            idField.setAccessible(true);
            ReflectionUtils.setField(idField, task, 1L);

            TaskAssignment ownerAssignment = new TaskAssignment(task, owner, ETaskRole.ASSIGNEE);

            Set<TaskAssignment> assignments = new HashSet<>();
            assignments.add(ownerAssignment);

            Field assignmentsField = Task.class.getDeclaredField("assignments");
            assignmentsField.setAccessible(true);
            ReflectionUtils.setField(assignmentsField, task, assignments);

        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Failed to set field on Task mock via reflection", e);
        }

        return task;
    }

    public static TaskRequestRegisterDTO createTaskRequestRegisterDTO() {
        return new TaskRequestRegisterDTO("Task mock", "Task mock description", OffsetDateTime.now().plusDays(1));
    }

    public static UpdateTaskStatusRequestDTO createUpdateTaskStatusRequestDTO(ETaskStatus taskStatus, String completionComment) {
        return new UpdateTaskStatusRequestDTO(taskStatus, completionComment);
    }

    public static UpdateTaskStatusRequestDTO createUpdateTaskStatusRequestDTO(ETaskStatus taskStatus) {
        return new UpdateTaskStatusRequestDTO(taskStatus, null);
    }

    public static Set<TaskAssignmentRequestDTO> createTaskAssignmentRequestDTOSet() {
        Set<TaskAssignmentRequestDTO> assignmentsUsers = new HashSet<>();
        assignmentsUsers.add(new TaskAssignmentRequestDTO(1L, ETaskRole.ASSIGNEE));
        assignmentsUsers.add(new TaskAssignmentRequestDTO(2L, ETaskRole.ASSIGNEE));
        return assignmentsUsers;
    }

    public static Set<User> createAssignmentusers(List<Long> usersIds) {
        Set<User> assignmetnsUsers = new HashSet<>();;

        for (Long id : usersIds) {
            assignmetnsUsers.add(createUserWithId(id, "testuser+" + id, "test+" + id + "@example.com"));
        }

        return assignmetnsUsers;
    }

    public static Set<UserAssignmentResponseDTO> createAssignmentusersDTO(Set<User> users) {
        Set<UserAssignmentResponseDTO> assignmetnsUsers = new HashSet<>();;

        for (User user : users) {
            assignmetnsUsers.add(new UserAssignmentResponseDTO(user.getId(), user.getUsername(), ETaskRole.ASSIGNEE));
        }

        return assignmetnsUsers;
    }
}
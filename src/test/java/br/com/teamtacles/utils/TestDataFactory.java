package br.com.teamtacles.utils;

import br.com.teamtacles.project.dto.request.InviteProjectMemberRequestDTO;
import br.com.teamtacles.project.dto.request.ProjectRequestUpdateDTO;
import br.com.teamtacles.project.enumeration.EProjectRole;
import br.com.teamtacles.project.model.Project;
import br.com.teamtacles.project.model.ProjectMember;
import br.com.teamtacles.team.dto.request.InvitedMemberRequestDTO;
import br.com.teamtacles.team.dto.request.TeamRequestRegisterDTO;
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

public class TestDataFactory {

    // User Factory Methods

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
        return new UserRequestRegisterDTO(
                "testuser",
                "test@gmail.com",
                "Password123",
                "Password123"
        );
    }

    // Team Factory Methods

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
        TeamRequestRegisterDTO dto = new TeamRequestRegisterDTO();
        dto.setName("New Awesome Team");
        dto.setDescription("Description for the new team.");
        return dto;
    }


    public static InvitedMemberRequestDTO createInviteMemberRequestDTO(String email, ETeamRole role) {
        InvitedMemberRequestDTO dto = new InvitedMemberRequestDTO();
        dto.setEmail(email);
        dto.setRole(role);
        return dto;
    }


    public static UpdateMemberRoleTeamRequestDTO createUpdateMemberRoleRequestDTO(ETeamRole newRole) {
        UpdateMemberRoleTeamRequestDTO dto = new UpdateMemberRoleTeamRequestDTO();
        dto.setNewRole(newRole);
        return dto;
    }

    public static Project createMockProject(User owner) {
        Project project = new Project("Mock Project", "A mock project for testing purposes.", owner);
        try {
            Field idField = Project.class.getDeclaredField("id");
            idField.setAccessible(true);
            ReflectionUtils.setField(idField, project, 100L);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Failed to set ID on Project mock", e);
        }

        ProjectMember ownerMembership = new ProjectMember(owner, project, EProjectRole.OWNER);
        ownerMembership.acceptedInvitation();

        try {
            Field memberIdField = ProjectMember.class.getDeclaredField("id");
            memberIdField.setAccessible(true);
            ReflectionUtils.setField(memberIdField, ownerMembership, 999L);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Failed to set ID on ProjectMember mock", e);
        }
        project.addMember(ownerMembership);
        return project;
    }
    public static ProjectRequestUpdateDTO createProjectRequestUpdateDTO() {
        return new ProjectRequestUpdateDTO("Updated Project Title", "Updated Description");
    }
    public static InviteProjectMemberRequestDTO createInviteProjectMemberRequestDTO(String email, EProjectRole role) {
        InviteProjectMemberRequestDTO dto = new InviteProjectMemberRequestDTO();
        dto.setEmail(email);
        dto.setRole(role);
        return dto;
    }

}
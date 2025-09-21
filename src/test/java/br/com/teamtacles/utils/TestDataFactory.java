package br.com.teamtacles.utils;

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
        user.setPassword("encodedPassword123");
        user.setEnabled(true);
        return user;
    }

    public static User createUnverifiedUser() {
        User user = createValidUser();
        user.setEnabled(false);
        user.setVerificationToken("valid-token-123");
        user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(1));
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
        Team team = new Team();
        try {
            Field idField = Team.class.getDeclaredField("id");
            idField.setAccessible(true);
            ReflectionUtils.setField(idField, team, 1L);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        team.setName("Team Tacles");
        team.setDescription("A great team");
        team.setOwner(owner);

        TeamMember ownerMembership = new TeamMember(owner, team, ETeamRole.OWNER);
        ownerMembership.setAcceptedInvite(true);
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
        member.setAcceptedInvite(true);
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
}
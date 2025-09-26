package br.com.teamtacles.team.service;

import br.com.teamtacles.common.exception.ResourceAlreadyExistsException;
import br.com.teamtacles.infrastructure.email.EmailService;
import br.com.teamtacles.team.dto.request.InvitedMemberRequestDTO;
import br.com.teamtacles.team.dto.request.TeamRequestRegisterDTO;
import br.com.teamtacles.team.dto.request.UpdateMemberRoleTeamRequestDTO;
import br.com.teamtacles.team.dto.response.TeamResponseDTO;
import br.com.teamtacles.team.enumeration.ETeamRole;
import br.com.teamtacles.team.model.Team;
import br.com.teamtacles.team.model.TeamMember;
import br.com.teamtacles.team.repository.TeamMemberRepository;
import br.com.teamtacles.team.repository.TeamRepository;
import br.com.teamtacles.team.validator.MembershipValidator;
import br.com.teamtacles.team.validator.TeamMembershipActionValidator;
import br.com.teamtacles.team.validator.TeamNameUniquenessValidator;
import br.com.teamtacles.user.model.User;
import br.com.teamtacles.user.service.UserService;
import br.com.teamtacles.utils.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.AccessDeniedException;
import br.com.teamtacles.team.validator.TeamInvitationValidator;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class TeamServiceTest {

    @Mock
    private TeamRepository teamRepository;
    @Mock
    private TeamMemberRepository teamMemberRepository;
    @Mock
    private TeamAuthorizationService teamAuthorizationService;
    @Mock
    private TeamNameUniquenessValidator teamNameUniquenessValidator;
    @Mock
    private MembershipValidator membershipValidator;
    @Mock
    private TeamMembershipActionValidator teamMembershipActionValidator;
    @Mock
    private UserService userService;
    @Mock
    private EmailService emailService;
    @Mock
    private ModelMapper modelMapper;

    @Mock
    private TeamInvitationValidator teamInvitationValidator;

    @InjectMocks
    private TeamService teamService;

    @Nested
    @DisplayName("1. Team Creation Tests")
    class TeamCreationTests {
        @Test
        @DisplayName("1.1 - shouldCreateTeamSuccessfully_whenDataIsValid")
        void shouldCreateTeamSuccessfully_whenDataIsValid() {
            // Arrange
            User owner = TestDataFactory.createValidUser();
            TeamRequestRegisterDTO dto = TestDataFactory.createTeamRequestRegisterDTO();

            when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> {
                Team savedTeam = invocation.getArgument(0);
                Field idField = ReflectionUtils.findField(Team.class, "id");
                ReflectionUtils.makeAccessible(idField);
                ReflectionUtils.setField(idField, savedTeam, 1L);
                return savedTeam;
            });

            when(modelMapper.map(any(Team.class), eq(TeamResponseDTO.class))).thenReturn(new TeamResponseDTO());

            ArgumentCaptor<Team> teamCaptor = ArgumentCaptor.forClass(Team.class);

            // Act
            teamService.createTeam(dto, owner);

            // Assert
            verify(teamNameUniquenessValidator).validate(dto.getName(), owner);
            verify(teamRepository).save(teamCaptor.capture());
            Team savedTeam = teamCaptor.getValue();

            assertThat(savedTeam.getOwner()).isEqualTo(owner);
            assertThat(savedTeam.getMembers()).hasSize(1);
            TeamMember ownerMember = savedTeam.getMembers().iterator().next();
            assertThat(ownerMember.getUser()).isEqualTo(owner);
            assertThat(ownerMember.getTeamRole()).isEqualTo(ETeamRole.OWNER);
            assertThat(ownerMember.isAcceptedInvite()).isTrue();
        }


        @Test
        @DisplayName("1.2 - shouldThrowException_whenTeamNameAlreadyExists")
        void shouldThrowException_whenTeamNameAlreadyExists() {

            //Arrange
            User owner = TestDataFactory.createValidUser();
            TeamRequestRegisterDTO dto = TestDataFactory.createTeamRequestRegisterDTO();
            doThrow(new ResourceAlreadyExistsException("Team name already in use"))
                    .when(teamNameUniquenessValidator).validate(dto.getName(), owner);

            // Act & Assert
            assertThatThrownBy(() -> teamService.createTeam(dto, owner))
                    .isInstanceOf(ResourceAlreadyExistsException.class);

            verify(teamRepository, never()).save(any());


        }

    }

    @Nested
    @DisplayName("2. Team Invitation Tests")
    class TeamInvitationTests {

        @Test
        @DisplayName("2.1 -shouldInviteMemberSuccessfully_whenInviterIsOwner")
        void shouldInviteMemberSuccessfully_whenInviterIsOwner() {
            //Arrange
            User owner = TestDataFactory.createUserWithId(1L, "owner", "owner@gmail.com");
            User userToInvite = TestDataFactory.createUserWithId(2L, "newUser", "newuser@gmail.com");
            Team team = TestDataFactory.createTeam(owner);
            InvitedMemberRequestDTO inviteDTO = TestDataFactory.createInviteMemberRequestDTO(userToInvite.getEmail(), ETeamRole.MEMBER);

            when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));
            when(userService.findUserEntityByEmail(userToInvite.getEmail())).thenReturn(userToInvite);

            doNothing().when(teamInvitationValidator).validateRole(any(ETeamRole.class));

            //Act
            teamService.inviteMember(team.getId(), inviteDTO, owner);

            // Assert
            verify(teamAuthorizationService).checkTeamAdmin(owner, team);
            verify(membershipValidator).validateNewMember(userToInvite, team);
            verify(teamRepository).save(any(Team.class));
            verify(emailService).sendTeamInvitationEmail(eq(userToInvite.getEmail()), eq(team.getName()), anyString());
        }

        @Test
        @DisplayName("2.2 -shouldThrowException_whenInviterIsNotAdmin")
        void shouldThrowException_whenInviterIsNotAdmin() {
            // Arrange
            User member = TestDataFactory.createUserWithId(2L, "member", "member@gmail.com");
            User owner = TestDataFactory.createValidUser();
            Team team = TestDataFactory.createTeam(owner);
            InvitedMemberRequestDTO inviteDTO = new InvitedMemberRequestDTO();

            when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));
            doThrow(new AccessDeniedException("Permission denied.")).when(teamAuthorizationService).checkTeamAdmin(member, team);

            // Act & Assert
            assertThatThrownBy(() -> teamService.inviteMember(team.getId(), inviteDTO, member))
                    .isInstanceOf(AccessDeniedException.class);

            verify(teamRepository, never()).save(any());
            verify(emailService, never()).sendTeamInvitationEmail(any(), any(), any());
        }

        @Test
        @DisplayName("2.3 - shouldThrowException_whenInvitedUserIsAlreadyMember")
        void shouldThrowException_whenInvitedUserIsAlreadyMember() {
            // Arrange
            User owner = TestDataFactory.createValidUser();
            User alreadyMember = TestDataFactory.createUserWithId(2L, "member", "member@gmail.com");
            Team team = TestDataFactory.createTeam(owner);
            team.addMember(TestDataFactory.createTeamMember(alreadyMember, team, ETeamRole.MEMBER));
            InvitedMemberRequestDTO inviteDTO = new InvitedMemberRequestDTO();
            inviteDTO.setEmail(alreadyMember.getEmail());
            inviteDTO.setRole(ETeamRole.MEMBER);

            when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));
            when(userService.findUserEntityByEmail(alreadyMember.getEmail())).thenReturn(alreadyMember);
            doThrow(new ResourceAlreadyExistsException("User is already a member."))
                    .when(membershipValidator).validateNewMember(alreadyMember, team);

            // Act & Assert
            assertThatThrownBy(() -> teamService.inviteMember(team.getId(), inviteDTO, owner))
                    .isInstanceOf(ResourceAlreadyExistsException.class);

            verify(teamRepository, never()).save(any());
            verify(emailService, never()).sendTeamInvitationEmail(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("3. Member Role Update Tests")
    class UpdateMemberRoleTests {

        @Test
        @DisplayName("3.1 shouldUpdateMemberRole_whenOwnerPromotesToAdmin")
        void shouldUpdateMemberRole_whenOwnerPromotesToAdmin() {
            // Arrange
            User owner = TestDataFactory.createUserWithId(1L, "owner", "owner@mail.com");
            User memberToUpdate = TestDataFactory.createUserWithId(2L, "member", "member@mail.com");
            Team team = TestDataFactory.createTeam(owner);
            TeamMember ownerMembership = team.getMembers().iterator().next();
            TeamMember memberToUpdateMembership = TestDataFactory.createTeamMember(memberToUpdate, team, ETeamRole.MEMBER);
            UpdateMemberRoleTeamRequestDTO roleDto = TestDataFactory.createUpdateMemberRoleRequestDTO(ETeamRole.ADMIN);

            when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));
            when(userService.findUserEntityById(memberToUpdate.getId())).thenReturn(memberToUpdate);
            when(teamMemberRepository.findByUserAndTeam(owner, team)).thenReturn(Optional.of(ownerMembership));
            when(teamMemberRepository.findByUserAndTeam(memberToUpdate, team)).thenReturn(Optional.of(memberToUpdateMembership));

            when(teamMemberRepository.save(any(TeamMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            teamService.updateMemberRole(team.getId(), memberToUpdate.getId(), roleDto, owner);

            // Assert
            verify(teamMembershipActionValidator).validateRoleUpdate(ownerMembership, memberToUpdateMembership, ETeamRole.ADMIN);
            verify(teamMemberRepository).save(memberToUpdateMembership);
            assertThat(memberToUpdateMembership.getTeamRole()).isEqualTo(ETeamRole.ADMIN);

        }

        @Test
        @DisplayName("3.2 - shouldThrowException_whenAdminTriesToChangeAnotherAdminRole")
        void shouldThrowException_whenAdminTriesToChangeAnotherAdminRole() {
            // Arrange
            User owner = TestDataFactory.createUserWithId(1L, "owner", "owner@gmail.com");
            User actingAdmin = TestDataFactory.createUserWithId(2L, "admin1", "admin1@gmail.com");
            User adminToUpdate = TestDataFactory.createUserWithId(3L, "admin2", "admin2@gmail.com");
            Team team = TestDataFactory.createTeam(owner);
            TeamMember actingAdminMembership = TestDataFactory.createTeamMember(actingAdmin, team, ETeamRole.ADMIN);
            TeamMember adminToUpdateMembership = TestDataFactory.createTeamMember(adminToUpdate, team, ETeamRole.ADMIN);
            UpdateMemberRoleTeamRequestDTO roleDto = new UpdateMemberRoleTeamRequestDTO();
            roleDto.setNewRole(ETeamRole.MEMBER);

            when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));
            when(userService.findUserEntityById(adminToUpdate.getId())).thenReturn(adminToUpdate);
            when(teamMemberRepository.findByUserAndTeam(actingAdmin, team)).thenReturn(Optional.of(actingAdminMembership));
            when(teamMemberRepository.findByUserAndTeam(adminToUpdate, team)).thenReturn(Optional.of(adminToUpdateMembership));
            doThrow(new AccessDeniedException("Only the OWNER can change an ADMIN's role."))
                    .when(teamMembershipActionValidator).validateRoleUpdate(actingAdminMembership, adminToUpdateMembership, ETeamRole.MEMBER);

            // Act & Assert
            assertThatThrownBy(() -> teamService.updateMemberRole(team.getId(), adminToUpdate.getId(), roleDto, actingAdmin))
                    .isInstanceOf(AccessDeniedException.class);

            verify(teamMemberRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("4. Delete Membership Tests")
    class DeleteMembershipTests {

        @Test
        @DisplayName("4.1 - shouldThrowException_whenOwnerTriesToRemoveThemselves")
        void shouldThrowException_whenOwnerTriesToRemoveThemselves() {
            // Arrange
            User owner = TestDataFactory.createValidUser();
            Team team = TestDataFactory.createTeam(owner);
            TeamMember ownerMembership = team.getMembers().iterator().next();

            when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));
            when(userService.findUserEntityById(owner.getId())).thenReturn(owner);
            when(teamMemberRepository.findByUserAndTeam(owner, team)).thenReturn(Optional.of(ownerMembership));
            doThrow(new AccessDeniedException("OWNER cannot remove themselves."))
                    .when(teamMembershipActionValidator).validateDeletion(ownerMembership, ownerMembership);

            // Act & Assert
            assertThatThrownBy(() -> teamService.deleteMembershipFromTeam(team.getId(), owner.getId(), owner))
                    .isInstanceOf(AccessDeniedException.class);

            verify(teamRepository, never()).save(team);
        }

    }
}





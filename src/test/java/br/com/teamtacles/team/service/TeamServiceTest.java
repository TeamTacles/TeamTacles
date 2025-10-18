package br.com.teamtacles.team.service;

import br.com.teamtacles.common.dto.response.InviteLinkResponseDTO;
import br.com.teamtacles.common.exception.ResourceAlreadyExistsException;
import br.com.teamtacles.common.exception.ResourceNotFoundException;
import br.com.teamtacles.infrastructure.email.EmailService;
import br.com.teamtacles.team.dto.request.InvitedMemberRequestDTO;
import br.com.teamtacles.team.dto.request.TeamRequestRegisterDTO;
import br.com.teamtacles.team.dto.request.TeamRequestUpdateDTO;
import br.com.teamtacles.team.dto.request.UpdateMemberRoleTeamRequestDTO;
import br.com.teamtacles.team.dto.response.TeamMemberResponseDTO;
import br.com.teamtacles.team.dto.response.TeamResponseDTO;
import br.com.teamtacles.team.enumeration.ETeamRole;
import br.com.teamtacles.team.model.Team;
import br.com.teamtacles.team.model.TeamMember;
import br.com.teamtacles.team.repository.TeamMemberRepository;
import br.com.teamtacles.team.repository.TeamRepository;
import br.com.teamtacles.team.validator.TeamMembershipValidator;
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

import br.com.teamtacles.team.validator.TeamTokenValidator;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    private TeamMembershipValidator teamMembershipValidator;
    @Mock
    private TeamMembershipActionValidator teamMembershipActionValidator;

    @Mock
    private TeamTokenValidator teamTokenValidator;

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
            teamService.inviteMemberByEmail(team.getId(), inviteDTO, owner);

            // Assert
            verify(teamAuthorizationService).checkTeamAdmin(owner, team);
            verify(teamMembershipValidator).validateNewMember(userToInvite, team);
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
            assertThatThrownBy(() -> teamService.inviteMemberByEmail(team.getId(), inviteDTO, member))
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
                    .when(teamMembershipValidator).validateNewMember(alreadyMember, team);

            // Act & Assert
            assertThatThrownBy(() -> teamService.inviteMemberByEmail(team.getId(), inviteDTO, owner))
                    .isInstanceOf(ResourceAlreadyExistsException.class);

            verify(teamRepository, never()).save(any());
            verify(emailService, never()).sendTeamInvitationEmail(any(), any(), any());
        }

        @Test
        @DisplayName("2.4 - acceptInvitationFromEmail_whenTokenIsExpired_shouldThrowException")
        void acceptInvitationFromEmail_whenTokenIsExpired_shouldThrowException() {
            // Arrange
            User owner = TestDataFactory.createValidUser();
            User invitee = TestDataFactory.createUserWithId(2L, "invitee", "invitee@example.com");
            Team team = TestDataFactory.createTeam(owner);

            TeamMember memberWithExpiredToken = TestDataFactory.createTeamMemberWithExpiredInvitation(invitee, team, ETeamRole.MEMBER);
            String expiredToken = memberWithExpiredToken.getInvitationToken();

            when(teamMemberRepository.findByInvitationToken(expiredToken)).thenReturn(Optional.of(memberWithExpiredToken));
            doThrow(new ResourceNotFoundException("Invitation token has expired."))
                    .when(teamTokenValidator).validateInvitationToken(memberWithExpiredToken);

            // Act & Assert
            assertThrows(
                    ResourceNotFoundException.class,
                    () -> teamService.acceptInvitationFromEmail(expiredToken)
            );

            verify(teamMemberRepository, never()).save(any(TeamMember.class));
        }

        @Test
        @DisplayName("2.5 generateInvitedLink_whenUserIsAdmin_shouldGenerateLinkSuccessfully")
        void generateInvitedLink_whenUserIsAdmin_shouldGenerateLinkSuccessfully() {
            // Arrange
            User owner = TestDataFactory.createValidUser();
            User adminUser = TestDataFactory.createUserWithId(2L, "admin", "admin@example.com");
            Team team = TestDataFactory.createTeam(owner);
            team.addMember(TestDataFactory.createTeamMember(adminUser, team, ETeamRole.ADMIN));

            // Injeta o valor da property app.base-url no serviço mockado
            try {
                Field baseUrlField = TeamService.class.getDeclaredField("baseUrl");
                baseUrlField.setAccessible(true);
                ReflectionUtils.setField(baseUrlField, teamService, "http://localhost:8080");
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
            when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));
            doNothing().when(teamAuthorizationService).checkTeamAdmin(adminUser, team);
            when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));
            ArgumentCaptor<Team> teamCaptor = ArgumentCaptor.forClass(Team.class);

            // Act
            InviteLinkResponseDTO response = teamService.generateInvitedLink(team.getId(), adminUser);

            // Assert
            verify(teamRepository).save(teamCaptor.capture());
            Team savedTeam = teamCaptor.getValue();
            assertThat(savedTeam.getInvitationToken()).isNotNull().isNotBlank();
            assertThat(savedTeam.getInvitationTokenExpiry()).isNotNull();
            assertThat(response.getInviteLink()).isEqualTo("http://localhost:8080/api/team/join?token=" + savedTeam.getInvitationToken());
            assertThat(response.getExpiresAt()).isEqualTo(savedTeam.getInvitationTokenExpiry());
        }

        @Test
        @DisplayName("2.6 - generateInvitedLink_whenUserIsRegularMember_shouldThrowAccessDeniedException")
        void generateInvitedLink_whenUserIsRegularMember_shouldThrowAccessDeniedException() {
            // Arrange
            User owner = TestDataFactory.createValidUser();
            User regularMember = TestDataFactory.createUserWithId(2L, "member", "member@example.com");
            Team team = TestDataFactory.createTeam(owner);
            team.addMember(TestDataFactory.createTeamMember(regularMember, team, ETeamRole.MEMBER));

            when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));
            doThrow(new AccessDeniedException("Permission denied. Action requires ADMIN or OWNER role."))
                    .when(teamAuthorizationService).checkTeamAdmin(regularMember, team);

            // Act & Assert
            assertThrows(
                    AccessDeniedException.class,
                    () -> teamService.generateInvitedLink(team.getId(), regularMember)
            );
            verify(teamRepository, never()).save(any(Team.class));
        }
        @Test
        @DisplayName("2.8 - acceptInvitationFromLink_whenTokenIsValid_shouldAddUserToTeam")
        void acceptInvitationFromLink_whenTokenIsValid_shouldAddUserToTeam() {
            // Arrange
            User owner = TestDataFactory.createValidUser();
            Team teamWithInvite = TestDataFactory.createTeamWithActiveInviteLink(owner);
            String validToken = teamWithInvite.getInvitationToken();
            User newUser = TestDataFactory.createUserWithId(3L, "newUser", "new@example.com");

            when(teamRepository.findByInvitationToken(validToken)).thenReturn(Optional.of(teamWithInvite));
            doNothing().when(teamTokenValidator).validateInvitationLinkToken(teamWithInvite);
            doNothing().when(teamMembershipValidator).validateNewMember(newUser, teamWithInvite);
            ArgumentCaptor<Team> teamCaptor = ArgumentCaptor.forClass(Team.class);
            when(teamRepository.save(teamCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            TeamMemberResponseDTO response = teamService.acceptInvitationFromLink(validToken, newUser);

            // Assert
            Team savedTeam = teamCaptor.getValue();
            Optional<TeamMember> addedMemberOpt = savedTeam.getMembers().stream()
                    .filter(member -> member.getUser().equals(newUser))
                    .findFirst();

            assertThat(addedMemberOpt).isPresent();
            TeamMember addedMember = addedMemberOpt.get();
            assertThat(addedMember.getTeamRole()).isEqualTo(ETeamRole.MEMBER);
            assertThat(addedMember.isAcceptedInvite()).isTrue();
            assertThat(response).isNotNull();
            assertThat(response.getUserId()).isEqualTo(newUser.getId());
        }
        @Test
        @DisplayName("2.9 - acceptInvitationFromLink_whenTokenIsInvalid_shouldThrowResourceNotFoundException")
        void acceptInvitationFromLink_whenTokenIsInvalid_shouldThrowResourceNotFoundException() {
            // Arrange
            String invalidToken = "um-token-que-nao-existe";
            User anyUser = TestDataFactory.createValidUser();
            when(teamRepository.findByInvitationToken(invalidToken)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(
                    ResourceNotFoundException.class,
                    () -> teamService.acceptInvitationFromLink(invalidToken, anyUser)
            );
            verify(teamTokenValidator, never()).validateInvitationLinkToken(any(Team.class));
            verify(teamMembershipValidator, never()).validateNewMember(any(User.class), any(Team.class));
            verify(teamRepository, never()).save(any(Team.class));
        }

        @Test
        @DisplayName("2.10 - acceptInvitationFromLink_whenUserIsAlreadyMember_shouldThrowResourceAlreadyExistsException")
        void acceptInvitationFromLink_whenUserIsAlreadyMember_shouldThrowResourceAlreadyExistsException() {
            // Arrange
            User owner = TestDataFactory.createValidUser();
            Team teamWithInvite = TestDataFactory.createTeamWithActiveInviteLink(owner);
            String validToken = teamWithInvite.getInvitationToken();
            User alreadyMember = TestDataFactory.createUserWithId(3L, "alreadyMember", "member@example.com");
            teamWithInvite.addMember(TestDataFactory.createTeamMember(alreadyMember, teamWithInvite, ETeamRole.MEMBER));
            when(teamRepository.findByInvitationToken(validToken)).thenReturn(Optional.of(teamWithInvite));
            doNothing().when(teamTokenValidator).validateInvitationLinkToken(teamWithInvite);
            doThrow(new ResourceAlreadyExistsException("User is already a member of this team."))
                    .when(teamMembershipValidator).validateNewMember(alreadyMember, teamWithInvite);
            // Act & Assert
            assertThrows(
                    ResourceAlreadyExistsException.class,
                    () -> teamService.acceptInvitationFromLink(validToken, alreadyMember)
            );

            verify(teamRepository, never()).save(any(Team.class));
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
            UpdateMemberRoleTeamRequestDTO roleDto = TestDataFactory.createUpdateMemberRoleTeamRequestDTO(ETeamRole.ADMIN);
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
        @Test
        @DisplayName("4.2 - deleteMembershipFromTeam_whenOwnerRemovesAdmin_shouldSucceed")
        void deleteMembershipFromTeam_whenOwnerRemovesAdmin_shouldSucceed() {
            // Arrange
            User owner = TestDataFactory.createValidUser();
            User adminToRemove = TestDataFactory.createUserWithId(2L, "admin", "admin@example.com");
            Team team = TestDataFactory.createTeam(owner);
            TeamMember ownerMembership = team.getMembers().stream().findFirst().get();
            TeamMember adminMembership = TestDataFactory.createTeamMember(adminToRemove, team, ETeamRole.ADMIN);
            team.addMember(adminMembership);
            assertThat(team.getMembers()).hasSize(2);
            when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));
            doNothing().when(teamAuthorizationService).checkTeamAdmin(owner, team);
            when(userService.findUserEntityById(adminToRemove.getId())).thenReturn(adminToRemove);
            when(teamMemberRepository.findByUserAndTeam(owner, team)).thenReturn(Optional.of(ownerMembership));
            when(teamMemberRepository.findByUserAndTeam(adminToRemove, team)).thenReturn(Optional.of(adminMembership));
            doNothing().when(teamMembershipActionValidator).validateDeletion(ownerMembership, adminMembership);
            ArgumentCaptor<Team> teamCaptor = ArgumentCaptor.forClass(Team.class);
            when(teamRepository.save(teamCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            teamService.deleteMembershipFromTeam(team.getId(), adminToRemove.getId(), owner);

            // Assert
            verify(teamAuthorizationService).checkTeamAdmin(owner, team);
            Team savedTeam = teamCaptor.getValue();

            assertThat(savedTeam.getMembers()).hasSize(1);
            assertThat(savedTeam.getMembers()).contains(ownerMembership);
        }
        @Test
        @DisplayName("4.3 - deleteMembershipFromTeam_whenAdminRemovesMember_shouldSucceed")
        void deleteMembershipFromTeam_whenAdminRemovesMember_shouldSucceed() {
            // Arrange
            User owner = TestDataFactory.createValidUser();
            User actingAdmin = TestDataFactory.createUserWithId(2L, "admin", "admin@example.com");
            User memberToRemove = TestDataFactory.createUserWithId(3L, "member", "member@example.com");
            Team team = TestDataFactory.createTeam(owner);
            TeamMember adminMembership = TestDataFactory.createTeamMember(actingAdmin, team, ETeamRole.ADMIN);
            TeamMember memberToRemoveMembership = TestDataFactory.createTeamMember(memberToRemove, team, ETeamRole.MEMBER);
            team.addMember(adminMembership);
            team.addMember(memberToRemoveMembership);
            assertThat(team.getMembers()).hasSize(3);
            when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));
            doNothing().when(teamAuthorizationService).checkTeamAdmin(actingAdmin, team);
            when(userService.findUserEntityById(memberToRemove.getId())).thenReturn(memberToRemove);
            when(teamMemberRepository.findByUserAndTeam(actingAdmin, team)).thenReturn(Optional.of(adminMembership));
            when(teamMemberRepository.findByUserAndTeam(memberToRemove, team)).thenReturn(Optional.of(memberToRemoveMembership));
            doNothing().when(teamMembershipActionValidator).validateDeletion(adminMembership, memberToRemoveMembership);
            ArgumentCaptor<Team> teamCaptor = ArgumentCaptor.forClass(Team.class);
            when(teamRepository.save(teamCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            teamService.deleteMembershipFromTeam(team.getId(), memberToRemove.getId(), actingAdmin);

            // Assert
            Team savedTeam = teamCaptor.getValue();

            assertThat(savedTeam.getMembers()).hasSize(2);
            assertThat(savedTeam.getMembers()).extracting(TeamMember::getUser)
                    .contains(owner, actingAdmin)
                    .doesNotContain(memberToRemove);
        }

    }
    @Nested
    @DisplayName("5. Team Deletion Tests")
    class TeamDeletionTests {

        @Test
        @DisplayName("5.1 - deleteTeam_whenUserIsOwner_shouldDeleteTeamSuccessfully")
        void deleteTeam_whenUserIsOwner_shouldDeleteTeamSuccessfully() {
            // Arrange
            User owner = TestDataFactory.createValidUser();
            Team team = TestDataFactory.createTeam(owner);

            when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));
            doNothing().when(teamAuthorizationService).checkTeamOwner(owner, team);
            doNothing().when(teamRepository).delete(team);

            ArgumentCaptor<Team> teamCaptor = ArgumentCaptor.forClass(Team.class);

            // Act
            teamService.deleteTeam(team.getId(), owner);

            // Assert
            verify(teamAuthorizationService).checkTeamOwner(owner, team);

            verify(teamRepository).delete(teamCaptor.capture());
            assertThat(teamCaptor.getValue()).isEqualTo(team);
        }
        @Test
        @DisplayName("5.2 - deleteTeam_whenUserIsAdminButNotOwner_shouldThrowAccessDeniedException")
        void deleteTeam_whenUserIsAdminButNotOwner_shouldThrowAccessDeniedException() {
            // Arrange
            User owner = TestDataFactory.createValidUser();
            User adminUser = TestDataFactory.createUserWithId(2L, "admin", "admin@example.com");
            Team team = TestDataFactory.createTeam(owner);
            team.addMember(TestDataFactory.createTeamMember(adminUser, team, ETeamRole.ADMIN));

            when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));
            doThrow(new AccessDeniedException("Permission denied. Action requires OWNER role."))
                    .when(teamAuthorizationService).checkTeamOwner(adminUser, team);

            // Act & Assert
            assertThrows(
                    AccessDeniedException.class,
                    () -> teamService.deleteTeam(team.getId(), adminUser)
            );

            verify(teamRepository, never()).delete(any(Team.class));
        }
    }
    @Nested
    @DisplayName("6. Team Update Tests")
    class TeamUpdateTests {

        @Test
        @DisplayName("6.1 - updateTeam_whenUserIsOwner_shouldUpdateSuccessfully")
        void updateTeam_whenUserIsOwner_shouldUpdateSuccessfully() {
            // Arrange
            User owner = TestDataFactory.createValidUser();
            Team team = TestDataFactory.createTeam(owner);
            TeamRequestUpdateDTO updateDTO = TestDataFactory.createTeamRequestUpdateDTO("New Team Name", "New Description");

            when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));
            doNothing().when(teamAuthorizationService).checkTeamOwner(owner, team);
            doNothing().when(teamNameUniquenessValidator).validate(updateDTO.getName(), owner);
            when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(modelMapper.map(any(Team.class), eq(TeamResponseDTO.class))).thenReturn(new TeamResponseDTO());

            ArgumentCaptor<Team> teamCaptor = ArgumentCaptor.forClass(Team.class);

            // Act
            teamService.updateTeam(team.getId(), updateDTO, owner);

            // Assert
            verify(teamRepository).save(teamCaptor.capture());
            Team savedTeam = teamCaptor.getValue();

            assertThat(savedTeam.getName()).isEqualTo("New Team Name");
            assertThat(savedTeam.getDescription()).isEqualTo("New Description");
        }

        @Test
        @DisplayName("6.2 - updateTeam_whenNewNameIsAlreadyTaken_shouldThrowResourceAlreadyExistsException")
        void updateTeam_whenNewNameIsAlreadyTaken_shouldThrowResourceAlreadyExistsException() {
            // Arrange
            User owner = TestDataFactory.createValidUser();
            Team team = TestDataFactory.createTeam(owner);
            TeamRequestUpdateDTO updateDTO = TestDataFactory.createTeamRequestUpdateDTO("Existing Name", "Any Description");

            when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));
            doNothing().when(teamAuthorizationService).checkTeamOwner(owner, team);
            doThrow(new ResourceAlreadyExistsException("Team name already in use by this creator."))
                    .when(teamNameUniquenessValidator).validate(updateDTO.getName(), owner);
            // Act & Assert
            assertThrows(
                    ResourceAlreadyExistsException.class,
                    () -> teamService.updateTeam(team.getId(), updateDTO, owner)
            );

            verify(teamRepository, never()).save(any(Team.class));
        }
    }
}





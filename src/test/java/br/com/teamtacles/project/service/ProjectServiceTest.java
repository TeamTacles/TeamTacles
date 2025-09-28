package br.com.teamtacles.project.service;

import br.com.teamtacles.common.exception.ResourceAlreadyExistsException;
import br.com.teamtacles.infrastructure.email.EmailService;
import br.com.teamtacles.project.dto.request.InviteProjectMemberRequestDTO;
import br.com.teamtacles.project.dto.request.ProjectRequestRegisterDTO;
import br.com.teamtacles.project.dto.request.ProjectRequestUpdateDTO;
import br.com.teamtacles.project.dto.response.ProjectResponseDTO;
import br.com.teamtacles.project.enumeration.EProjectRole;
import br.com.teamtacles.project.model.Project;
import br.com.teamtacles.project.model.ProjectMember;
import br.com.teamtacles.project.repository.ProjectMemberRepository;
import br.com.teamtacles.project.repository.ProjectRepository;
import br.com.teamtacles.project.validator.*;
import br.com.teamtacles.security.UserAuthenticated;
import br.com.teamtacles.user.model.User;
import br.com.teamtacles.user.service.UserService;
import br.com.teamtacles.utils.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import br.com.teamtacles.common.exception.ResourceNotFoundException;
import br.com.teamtacles.project.dto.request.UpdateMemberRoleProjectRequestDTO;
import br.com.teamtacles.project.validator.ProjectMembershipActionValidator;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ProjectMemberRepository projectMemberRepository;
    @Mock
    private ProjectTitleUniquenessValidator projectTitleUniquenessValidator;
    @Mock
    private ProjectMembershipValidator projectMembershipValidator;
    @Mock
    private ProjectInvitationValidator projectInvitationValidator;

    @Mock
    private ProjectTokenValidator projectTokenValidator;

    @Mock
    private UserService userService;
    @Mock
    private EmailService emailService;
    @Mock
    private UserAuthenticated userAuthenticated;
    @Mock
    private ModelMapper modelMapper;
    @Mock
    private ProjectAuthorizationService projectAuthorizationService;

    @Mock
    private ProjectMembershipActionValidator projectMembershipActionValidator;

    @InjectMocks
    private ProjectService projectService;

    @Captor
    private ArgumentCaptor<Project> projectCaptor;
    @Captor
    private ArgumentCaptor<ProjectMember> projectMemberCaptor;

    private User authenticatedUser;

    @BeforeEach
    void setUp() {
        authenticatedUser = TestDataFactory.createValidUser();
    }

    //pedron depois verificar esse teste juntamente com a entidade project e seu service
    @Test
    @DisplayName("createProject should create project with owner as member")
    void createProject_whenValidDataAndAuthenticatedUser_shouldCreateProjectWithOwnerMember() {
        // Arrange
        ProjectRequestRegisterDTO requestDTO = new ProjectRequestRegisterDTO("New Project", "Project Description");

        doNothing().when(projectTitleUniquenessValidator).validate(anyString(), any(User.class));

        Project savedProject = TestDataFactory.createMockProject(authenticatedUser);
        savedProject.setTitle(requestDTO.getTitle());
        savedProject.setDescription(requestDTO.getDescription());

        // Simular que o cascade funcionou - projeto salvo COM o membro
        ProjectMember ownerMember = new ProjectMember(authenticatedUser, savedProject, EProjectRole.OWNER);
        ownerMember.acceptedInvitation();
        savedProject.addMember(ownerMember);

        when(projectRepository.save(any(Project.class))).thenReturn(savedProject);

        ProjectResponseDTO expectedResponse = new ProjectResponseDTO();
        expectedResponse.setId(savedProject.getId());
        expectedResponse.setTitle(savedProject.getTitle());
        when(modelMapper.map(savedProject, ProjectResponseDTO.class)).thenReturn(expectedResponse);

        // Act
        ProjectResponseDTO actualResponse = projectService.createProject(requestDTO, authenticatedUser);

        // Assert
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getId()).isEqualTo(expectedResponse.getId());

        verify(projectTitleUniquenessValidator).validate(eq(requestDTO.getTitle()), eq(authenticatedUser));
        verify(projectRepository).save(projectCaptor.capture());

        Project capturedProject = projectCaptor.getValue();
        assertThat(capturedProject.getOwner()).isEqualTo(authenticatedUser);

        // Verifica que o membro foi adicionado AO AGREGADO
        assertThat(capturedProject.getMembers()).hasSize(1);
        ProjectMember addedMember = capturedProject.getMembers().stream()
                .findFirst()
                .orElseThrow();        assertThat(addedMember.getUser()).isEqualTo(authenticatedUser);
        assertThat(addedMember.getProjectRole()).isEqualTo(EProjectRole.OWNER);
        assertThat(addedMember.isAcceptedInvite()).isTrue();
        verifyNoInteractions(projectMemberRepository);
    }

    @Test
    @DisplayName("createProject should throw ResourceAlreadyExistsException when title already exists")
    void createProject_whenProjectTitleAlreadyExistsForUser_shouldThrowResourceAlreadyExistsException() {
        // Arrange
        ProjectRequestRegisterDTO requestDTO = new ProjectRequestRegisterDTO("Existing Project", "Some description");
        String expectedErrorMessage = "A project with this title already exists for this user.";

        doThrow(new ResourceAlreadyExistsException(expectedErrorMessage))
                .when(projectTitleUniquenessValidator).validate(anyString(), any(User.class));

        // Act & Assert
        ResourceAlreadyExistsException thrownException = assertThrows(
                ResourceAlreadyExistsException.class,
                () -> projectService.createProject(requestDTO, authenticatedUser)
        );

        assertThat(thrownException.getMessage()).isEqualTo(expectedErrorMessage);

        verifyNoInteractions(projectRepository);
        verifyNoInteractions(projectMemberRepository);
    }

    @Nested
    @DisplayName("Project Update Tests")
    class ProjectUpdateTests {

        @Test
        @DisplayName("should update and return ProjectResponseDTO when user is owner and data is valid")
        void updateProject_whenUserIsOwnerAndDataIsValid_shouldUpdateAndReturnProjectDTO() {
            // Arrange
            long projectId = 1L;
            ProjectRequestUpdateDTO requestDTO = TestDataFactory.createProjectRequestUpdateDTO();
            User owner = TestDataFactory.createValidUser();
            Project existingProject = TestDataFactory.createMockProject(owner);

            when(projectRepository.findById(projectId)).thenReturn(Optional.of(existingProject));
            doNothing().when(projectAuthorizationService).checkProjectAdmin(owner, existingProject);
            when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ProjectResponseDTO expectedResponse = new ProjectResponseDTO();
            expectedResponse.setId(existingProject.getId());
            expectedResponse.setTitle(requestDTO.getTitle());
            when(modelMapper.map(any(Project.class), eq(ProjectResponseDTO.class))).thenReturn(expectedResponse);

            // Act
            ProjectResponseDTO actualResponse = projectService.updateProject(projectId, requestDTO, owner);

            // Assert
            assertThat(actualResponse).isNotNull();
            assertThat(actualResponse.getTitle()).isEqualTo(expectedResponse.getTitle());
            verify(projectRepository).findById(projectId);
            verify(projectAuthorizationService).checkProjectAdmin(owner, existingProject);
            verify(projectRepository).save(projectCaptor.capture());
            Project capturedProject = projectCaptor.getValue();
            assertThat(capturedProject.getTitle()).isEqualTo(requestDTO.getTitle());
            assertThat(capturedProject.getDescription()).isEqualTo(requestDTO.getDescription());
        }

        @Test
        @DisplayName("should throw AccessDeniedException when user is not a project member")
        void updateProject_whenUserIsNotMember_shouldThrowAccessDeniedException() {
            // Arrange
            long projectId = 1L;
            ProjectRequestUpdateDTO requestDTO = TestDataFactory.createProjectRequestUpdateDTO();

            User owner = TestDataFactory.createUserWithId(1L, "owner", "owner@example.com");
            User nonMemberUser = TestDataFactory.createUserWithId(2L, "nonmember", "nonmember@example.com");

            Project existingProject = TestDataFactory.createMockProject(owner);
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(existingProject));
            doThrow(new AccessDeniedException("Access is denied"))
                    .when(projectAuthorizationService).checkProjectAdmin(nonMemberUser, existingProject);
            // Act & Assert
            assertThrows(
                    AccessDeniedException.class,
                    () -> projectService.updateProject(projectId, requestDTO, nonMemberUser)
            );
            verify(projectRepository, never()).save(any(Project.class));
        }

        @Test
        @DisplayName("should throw AccessDeniedException when user is a member but not admin or owner")
        void updateProject_whenUserIsMemberButNotAdminOrOwner_shouldThrowAccessDeniedException() {
            // Arrange
            long projectId = 1L;
            ProjectRequestUpdateDTO requestDTO = TestDataFactory.createProjectRequestUpdateDTO();

            User owner = TestDataFactory.createUserWithId(1L, "owner", "owner@example.com");
            User regularMemberUser = TestDataFactory.createUserWithId(3L, "member", "member@example.com");

            Project existingProject = TestDataFactory.createMockProject(owner);

            when(projectRepository.findById(projectId)).thenReturn(Optional.of(existingProject));

            doThrow(new AccessDeniedException("User does not have admin privileges for this project"))
                    .when(projectAuthorizationService).checkProjectAdmin(regularMemberUser, existingProject);

            // Act & Assert
            assertThrows(
                    AccessDeniedException.class,
                    () -> projectService.updateProject(projectId, requestDTO, regularMemberUser)
            );
            verify(projectRepository, never()).save(any(Project.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when project does not exist")
        void updateProject_whenProjectDoesNotExist_shouldThrowResourceNotFoundException() {
            // Arrange
            long nonExistentProjectId = 999L;
            ProjectRequestUpdateDTO requestDTO = TestDataFactory.createProjectRequestUpdateDTO();
            User actingUser = TestDataFactory.createValidUser();

            when(projectRepository.findById(nonExistentProjectId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(
                    ResourceNotFoundException.class,
                    () -> projectService.updateProject(nonExistentProjectId, requestDTO, actingUser)
            );

            verify(projectAuthorizationService, never()).checkProjectAdmin(any(User.class), any(Project.class));
            verify(projectRepository, never()).save(any(Project.class));
        }

    }

    @Nested
    @DisplayName("Project Deletion Tests")
    class ProjectDeletionTests {

        @Test
        @DisplayName("should delete project successfully when user is the owner")
        void deleteProject_whenUserIsOwner_shouldDeleteProjectSuccessfully() {
            // Arrange
            long projectId = 1L;
            User owner = TestDataFactory.createValidUser();
            Project existingProject = TestDataFactory.createMockProject(owner);

            when(projectRepository.findById(projectId)).thenReturn(Optional.of(existingProject));

            doNothing().when(projectAuthorizationService).checkProjectOwner(owner, existingProject);

            doNothing().when(projectRepository).delete(any(Project.class));

            // Act
            projectService.deleteProject(projectId, owner);

            // Assert
            verify(projectRepository).findById(projectId);
            verify(projectAuthorizationService).checkProjectOwner(owner, existingProject);
            verify(projectRepository).delete(projectCaptor.capture());
            assertThat(projectCaptor.getValue()).isEqualTo(existingProject);
        }

        @Test
        @DisplayName("should throw AccessDeniedException when user is admin but not the owner")
        void deleteProject_whenUserIsAdminButNotOwner_shouldThrowAccessDeniedException() {
            // Arrange
            long projectId = 1L;
            User owner = TestDataFactory.createUserWithId(1L, "owner", "owner@example.com");
            User adminUser = TestDataFactory.createUserWithId(4L, "admin", "admin@example.com");
            Project existingProject = TestDataFactory.createMockProject(owner);
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(existingProject));

            doThrow(new AccessDeniedException("Only the project owner can delete the project"))
                    .when(projectAuthorizationService).checkProjectOwner(adminUser, existingProject);

            // Act & Assert
            assertThrows(
                    AccessDeniedException.class,
                    () -> projectService.deleteProject(projectId, adminUser)
            );

            verify(projectRepository, never()).delete(any(Project.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when project does not exist")
        void deleteProject_whenProjectDoesNotExist_shouldThrowResourceNotFoundException() {
            // Arrange
            long nonExistentProjectId = 999L;
            User actingUser = TestDataFactory.createValidUser();
            when(projectRepository.findById(nonExistentProjectId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(
                    ResourceNotFoundException.class,
                    () -> projectService.deleteProject(nonExistentProjectId, actingUser)
            );
            verify(projectAuthorizationService, never()).checkProjectOwner(any(User.class), any(Project.class));
            verify(projectRepository, never()).delete(any(Project.class));
        }
    }
    @Nested
    @DisplayName("Project Member Management Tests")
    class ProjectMemberManagementTests {

        @Test
        @DisplayName("should create and send pending invite when user is admin and invitee is not a member")
        void inviteMember_whenUserIsAdminAndInviteeIsNotMember_shouldCreatePendingInvite() {
            // Arrange
            long projectId = 1L;
            User inviter = authenticatedUser;
            User invitee = TestDataFactory.createUserWithId(2L, "invitee", "invitee@example.com");
            Project project = TestDataFactory.createMockProject(inviter);
            InviteProjectMemberRequestDTO requestDTO = TestDataFactory.createInviteProjectMemberRequestDTO(invitee.getEmail(), EProjectRole.MEMBER);
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
            when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));
            doNothing().when(projectAuthorizationService).checkProjectAdmin(inviter, project);
            when(userService.findUserEntityByEmail(invitee.getEmail())).thenReturn(invitee);
            doNothing().when(projectInvitationValidator).validateRole(requestDTO.getRole());
            doNothing().when(projectMembershipValidator).validateNewMember(invitee, project);
            doNothing().when(emailService).sendProjectInvitationEmail(anyString(), anyString(), anyString());

            // Act
            projectService.inviteMemberByEmail(projectId, requestDTO, inviter);

            // Assert
            verify(projectRepository).save(projectCaptor.capture());
            Project capturedProject = projectCaptor.getValue();

            // Verifica se o projeto agora tem 2 membros (o dono original + o novo convidado)
            assertThat(capturedProject.getMembers()).hasSize(2);

            ProjectMember newMember = capturedProject.getMembers().stream()
                    .filter(member -> member.getUser().equals(invitee))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Novo membro não encontrado na lista do projeto"));

            assertThat(newMember.getUser()).isEqualTo(invitee);
            assertThat(newMember.getProjectRole()).isEqualTo(EProjectRole.MEMBER);
            assertThat(newMember.isAcceptedInvite()).isFalse();
            assertThat(newMember.getInvitationToken()).isNotNull().isNotBlank();

            verify(emailService).sendProjectInvitationEmail(
                    eq(invitee.getEmail()),
                    eq(project.getTitle()),
                    eq(newMember.getInvitationToken())
            );
        }

        @Test
        @DisplayName("should throw AccessDeniedException when inviting user is a regular member")
        void inviteMember_whenUserIsRegularMember_shouldThrowAccessDeniedException() {
            // Arrange
            long projectId = 1L;
            User owner = TestDataFactory.createUserWithId(1L, "owner", "owner@example.com");
            User regularMember = TestDataFactory.createUserWithId(2L, "member", "member@example.com");
            Project project = TestDataFactory.createMockProject(owner);
            InviteProjectMemberRequestDTO requestDTO = TestDataFactory.createInviteProjectMemberRequestDTO("new@example.com", EProjectRole.MEMBER);

            when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

            doThrow(new AccessDeniedException("Access is denied"))
                    .when(projectAuthorizationService).checkProjectAdmin(regularMember, project);

            // Act & Assert
            assertThrows(AccessDeniedException.class,
                    () -> projectService.inviteMemberByEmail(projectId, requestDTO, regularMember));
            verify(userService, never()).findUserEntityByEmail(anyString());
            verify(projectRepository, never()).save(any(Project.class));
            verifyNoInteractions(emailService);
        }

        @Test
        @DisplayName("acceptInvitationFromEmail_whenTokenIsValid_shouldUpdateMembershipToAccepted")
        void acceptInvitationFromEmail_whenTokenIsValid_shouldUpdateMembershipToAccepted() {
            // Arrange
            User owner = TestDataFactory.createValidUser();
            User invitee = TestDataFactory.createUserWithId(2L, "invitee", "invitee@example.com");
            Project project = TestDataFactory.createMockProject(owner);

            ProjectMember pendingMember = TestDataFactory.createPendingProjectMember(invitee, project, EProjectRole.MEMBER);
            String validToken = pendingMember.getInvitationToken();

            when(projectMemberRepository.findByInvitationToken(validToken)).thenReturn(Optional.of(pendingMember));
            doNothing().when(projectTokenValidator).validateInvitationEmailToken(pendingMember);
            when(projectMemberRepository.save(any(ProjectMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            projectService.acceptInvitationFromEmail(validToken);

            // Assert
            verify(projectMemberRepository).save(projectMemberCaptor.capture());
            ProjectMember savedMember = projectMemberCaptor.getValue();

            assertThat(savedMember.isAcceptedInvite()).isTrue();
            assertThat(savedMember.getInvitationToken()).isNull();
            assertThat(savedMember.getInvitationTokenExpiry()).isNull();

            verify(projectTokenValidator).validateInvitationEmailToken(pendingMember);
        }
        @Test
        @DisplayName("acceptInvitationFromEmail_whenTokenIsInvalid_shouldThrowResourceNotFoundException")
        void acceptInvitationFromEmail_whenTokenIsInvalid_shouldThrowResourceNotFoundException() {
            // Arrange
            String invalidToken = "non-existent-token-123";
            when(projectMemberRepository.findByInvitationToken(invalidToken)).thenReturn(Optional.empty());
            // Act & Assert
            ResourceNotFoundException exception = assertThrows(
                    ResourceNotFoundException.class,
                    () -> projectService.acceptInvitationFromEmail(invalidToken)
            );
            assertThat(exception.getMessage()).isEqualTo("Invalid invitation token.");
            verify(projectMemberRepository, never()).save(any(ProjectMember.class));
        }

        @Test
        @DisplayName("updateMemberRole_whenUserIsOwnerAndTargetIsMember_shouldUpdateRoleSuccessfully")
        void updateMemberRole_whenUserIsOwnerAndTargetIsMember_shouldUpdateRoleSuccessfully() {
            // Arrange
            long projectId = 1L;
            User owner = TestDataFactory.createValidUser(); // ID = 1L
            User memberToUpdate = TestDataFactory.createUserWithId(2L, "memberToUpdate", "member@example.com");
            Project project = TestDataFactory.createMockProject(owner);

            ProjectMember ownerMembership = project.getMembers().stream().findFirst().get();
            ProjectMember memberToUpdateMembership = new ProjectMember(memberToUpdate, project, EProjectRole.MEMBER);
            project.addMember(memberToUpdateMembership);

            UpdateMemberRoleProjectRequestDTO requestDTO = new UpdateMemberRoleProjectRequestDTO(EProjectRole.ADMIN);

            when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
            doNothing().when(projectAuthorizationService).checkProjectAdmin(owner, project);
            when(userService.findUserEntityById(memberToUpdate.getId())).thenReturn(memberToUpdate);
            when(projectMemberRepository.findByUserAndProject(owner, project)).thenReturn(Optional.of(ownerMembership));
            when(projectMemberRepository.findByUserAndProject(memberToUpdate, project)).thenReturn(Optional.of(memberToUpdateMembership));
            doNothing().when(projectMembershipActionValidator).validateRoleUpdate(ownerMembership, memberToUpdateMembership, EProjectRole.ADMIN);
            when(projectMemberRepository.save(any(ProjectMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            projectService.updateMemberRole(projectId, memberToUpdate.getId(), requestDTO, owner);

            // Assert
            verify(projectMemberRepository).save(projectMemberCaptor.capture());
            ProjectMember savedMember = projectMemberCaptor.getValue();
            assertThat(savedMember.getProjectRole()).isEqualTo(EProjectRole.ADMIN);
            verify(projectAuthorizationService).checkProjectAdmin(owner, project);
            verify(projectMembershipActionValidator).validateRoleUpdate(ownerMembership, memberToUpdateMembership, EProjectRole.ADMIN);
        }

        @Test
        @DisplayName("updateMemberRole_whenUserIsAdminAndTargetIsOwner_shouldThrowAccessDeniedException")
        void updateMemberRole_whenUserIsAdminAndTargetIsOwner_shouldThrowAccessDeniedException() {
            // Arrange
            long projectId = 1L;
            User owner = TestDataFactory.createValidUser(); // ID = 1L
            User adminUser = TestDataFactory.createUserWithId(3L, "adminUser", "admin@example.com");
            Project project = TestDataFactory.createMockProject(owner);
            ProjectMember ownerMembership = project.getMembers().stream().findFirst().get();
            ProjectMember adminMembership = new ProjectMember(adminUser, project, EProjectRole.ADMIN);
            project.addMember(adminMembership);
            UpdateMemberRoleProjectRequestDTO requestDTO = new UpdateMemberRoleProjectRequestDTO(EProjectRole.MEMBER);
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
            doNothing().when(projectAuthorizationService).checkProjectAdmin(adminUser, project);
            when(userService.findUserEntityById(owner.getId())).thenReturn(owner);
            when(projectMemberRepository.findByUserAndProject(adminUser, project)).thenReturn(Optional.of(adminMembership));
            when(projectMemberRepository.findByUserAndProject(owner, project)).thenReturn(Optional.of(ownerMembership));
            doThrow(new AccessDeniedException("The project OWNER's role cannot be changed."))
                    .when(projectMembershipActionValidator)
                    .validateRoleUpdate(adminMembership, ownerMembership, EProjectRole.MEMBER);
            // Act & Assert
            assertThrows(
                    AccessDeniedException.class,
                    () -> projectService.updateMemberRole(projectId, owner.getId(), requestDTO, adminUser)
            );

            verify(projectMemberRepository, never()).save(any(ProjectMember.class));
        }

        @Test
        @DisplayName("removeMember_whenUserIsOwnerAndTargetIsMember_shouldRemoveMemberSuccessfully")
        void removeMember_whenUserIsOwnerAndTargetIsMember_shouldRemoveMemberSuccessfully() {
            // Arrange
            long projectId = 1L;
            User owner = TestDataFactory.createValidUser(); // ID = 1L
            User memberToRemove = TestDataFactory.createUserWithId(2L, "memberToRemove", "member@example.com");
            Project project = TestDataFactory.createMockProject(owner);
            ProjectMember ownerMembership = project.getMembers().stream().findFirst().get();
            ProjectMember memberToRemoveMembership = new ProjectMember(memberToRemove, project, EProjectRole.MEMBER);
            project.addMember(memberToRemoveMembership);
            assertThat(project.getMembers()).hasSize(2);
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
            doNothing().when(projectAuthorizationService).checkProjectAdmin(owner, project);
            when(userService.findUserEntityById(memberToRemove.getId())).thenReturn(memberToRemove);
            when(projectMemberRepository.findByUserAndProject(owner, project)).thenReturn(Optional.of(ownerMembership));
            when(projectMemberRepository.findByUserAndProject(memberToRemove, project)).thenReturn(Optional.of(memberToRemoveMembership));
            doNothing().when(projectMembershipActionValidator).validateDeletion(ownerMembership, memberToRemoveMembership);
            when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            projectService.deleteMembershipFromProject(projectId, memberToRemove.getId(), owner);

            // Assert
            verify(projectRepository).save(projectCaptor.capture());
            Project savedProject = projectCaptor.getValue();
            assertThat(savedProject.getMembers()).hasSize(1);
            assertThat(savedProject.getMembers()).extracting(ProjectMember::getUser).doesNotContain(memberToRemove);
            assertThat(savedProject.getMembers()).extracting(ProjectMember::getUser).contains(owner);
            verify(projectMembershipActionValidator).validateDeletion(ownerMembership, memberToRemoveMembership);
        }



    }
}
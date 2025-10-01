package br.com.teamtacles.task.service;

import br.com.teamtacles.project.enumeration.EProjectRole;
import br.com.teamtacles.project.model.Project;
import br.com.teamtacles.project.model.ProjectMember;
import br.com.teamtacles.project.repository.ProjectMemberRepository;
import br.com.teamtacles.project.service.ProjectAuthorizationService;
import br.com.teamtacles.task.model.Task;
import br.com.teamtacles.user.model.User;
import br.com.teamtacles.utils.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskAuthorizationServiceTest {

    @Mock
    private ProjectAuthorizationService projectAuthorizationService;
    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @InjectMocks
    private TaskAuthorizationService taskAuthorizationService;

    private User user;
    private Project project;
    private Task task;

    @BeforeEach
    void setUp() {
        user = TestDataFactory.createValidUser();
        project = mock(Project.class);
        task = mock(Task.class);
    }

    @Nested
    @DisplayName("1. View Permissions")
    class ViewPermissionsTests {
        @Test
        @DisplayName("1.1 - shouldAllowTaskView_WhenUserIsProjectMember")
        void shouldAllowTaskView_WhenUserIsProjectMember() {
            // ARRANGE
            when(task.getProject()).thenReturn(project);
            doNothing().when(projectAuthorizationService).checkProjectMembership(user, project);

            // ACT & ASSERT
            assertDoesNotThrow(() -> taskAuthorizationService.checkViewPermission(user, task));
        }

        @Test
        @DisplayName("1.2 - shouldDenyTaskView_WhenUserIsNotProjectMember")
        void shouldDenyTaskView_WhenUserIsNotProjectMember() {
            // ARRANGE - 
            when(task.getProject()).thenReturn(project);
            doThrow(new AccessDeniedException("Access denied."))
                    .when(projectAuthorizationService)
                    .checkProjectMembership(user, project);

            // ACT & ASSERT
            assertThrows(AccessDeniedException.class,
                    () -> taskAuthorizationService.checkViewPermission(user, task));
        }
    }

}
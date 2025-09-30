package br.com.teamtacles.task.service;

import br.com.teamtacles.common.exception.InvalidTaskStateException;
import br.com.teamtacles.common.exception.ResourceNotFoundException;
import br.com.teamtacles.project.model.Project;
import br.com.teamtacles.project.service.ProjectAuthorizationService;
import br.com.teamtacles.project.service.ProjectService;
import br.com.teamtacles.task.dto.request.TaskAssignmentRequestDTO;
import br.com.teamtacles.task.dto.request.TaskRequestRegisterDTO;
import br.com.teamtacles.task.dto.request.UpdateTaskStatusRequestDTO;
import br.com.teamtacles.task.dto.response.TaskResponseDTO;
import br.com.teamtacles.task.dto.response.TaskUpdateStatusResponseDTO;
import br.com.teamtacles.task.dto.response.UserAssignmentResponseDTO;
import br.com.teamtacles.task.enumeration.ETaskStatus;
import br.com.teamtacles.task.model.Task;
import br.com.teamtacles.task.repository.TaskRepository;
import br.com.teamtacles.task.validator.TaskAssignmentRoleValidator;
import br.com.teamtacles.task.validator.TaskProjectAssociationValidator;
import br.com.teamtacles.task.validator.TaskStateTransitionValidator;
import br.com.teamtacles.user.model.User;
import br.com.teamtacles.utils.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectService projectService;

    @Mock
    private ProjectAuthorizationService projectAuthorizationService;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private TaskStateTransitionValidator taskStateTransitionValidator;

    @Mock
    private TaskAuthorizationService taskAuthorizationService;

    @Mock
    private TaskProjectAssociationValidator taskProjectAssociationValidator;

    @Mock
    private TaskAssignmentRoleValidator taskAssignmentRoleValidator;

    @InjectMocks
    private TaskService taskService;

    private User taskCreator;
    private Project project;
    private TaskRequestRegisterDTO taskRequestDTO;

    @BeforeEach
    void setUp(){
        taskCreator = TestDataFactory.createValidUser();
        project = TestDataFactory.createMockProject(taskCreator);
        taskRequestDTO = TestDataFactory.createTaskRequestRegisterDTO();
    }

    @Nested
    @DisplayName("1. Task Creation Tests")
    class TaskCreationTests {

        @Test
        @DisplayName("1.1 - shouldCreateTaskSuccessfully_WhenUserIsProjectMember")
        void shouldCreateTaskSuccessfully_WhenUserIsProjectMember() {

            //ARRANGE
            when(projectService.findProjectEntityById(project.getId())).thenReturn(project);
            doNothing().when(projectAuthorizationService).checkProjectMembership(taskCreator, project);
            when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

            TaskResponseDTO taskResponseDTO = new TaskResponseDTO();
            taskResponseDTO.setId(1L);
            taskResponseDTO.setTitle(taskRequestDTO.getTitle());

            when(modelMapper.map(any(Task.class), eq(TaskResponseDTO.class))).thenReturn(taskResponseDTO);
            ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);

            //ACT
            TaskResponseDTO result = taskService.createTask(project.getId(), taskRequestDTO, taskCreator);

            //ASSERT
            verify(taskRepository, times(1)).save(taskCaptor.capture());
            Task savedTask = taskCaptor.getValue();

            assertNotNull(savedTask);
            assertEquals(taskRequestDTO.getTitle(), savedTask.getTitle());
            assertEquals(taskRequestDTO.getDueDate(), savedTask.getDueDate());
            assertEquals(taskRequestDTO.getDescription(), savedTask.getDescription());
        }

        @Test
        @DisplayName("1.2 - shouldSetCurrentUserAsTaskOwner_OnCreate")
        void shouldSetCurrentUserAsTaskOwner_OnCreate() {

            //ARRANGE
            when(projectService.findProjectEntityById(project.getId())).thenReturn(project);
            doNothing().when(projectAuthorizationService).checkProjectMembership(taskCreator, project);
            when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

            TaskResponseDTO taskResponseDTO = new TaskResponseDTO();
            taskResponseDTO.setId(1L);
            taskResponseDTO.setTitle(taskRequestDTO.getTitle());

            when(modelMapper.map(any(Task.class), eq(TaskResponseDTO.class))).thenReturn(taskResponseDTO);
            ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);

            //ACT
            TaskResponseDTO result = taskService.createTask(project.getId(), taskRequestDTO, taskCreator);

            //ASSERT
            verify(taskRepository, times(1)).save(taskCaptor.capture());
            Task savedTask = taskCaptor.getValue();

            assertNotNull(savedTask);
            assertEquals(taskCreator, savedTask.getOwner());
            assertEquals(taskCreator.getId(), savedTask.getOwner().getId());
        }

        @Test
        @DisplayName("1.3 - shouldSetInitialTaskStatusToToDo_OnCreate")
        void shouldSetInitialTaskStatusToToDo_OnCreate() {

            //ARRANGE
            when(projectService.findProjectEntityById(project.getId())).thenReturn(project);
            doNothing().when(projectAuthorizationService).checkProjectMembership(taskCreator, project);
            when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

            TaskResponseDTO taskResponseDTO = new TaskResponseDTO();
            taskResponseDTO.setId(1L);
            taskResponseDTO.setTitle(taskRequestDTO.getTitle());

            when(modelMapper.map(any(Task.class), eq(TaskResponseDTO.class))).thenReturn(taskResponseDTO);
            ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);

            //ACT
            TaskResponseDTO result = taskService.createTask(project.getId(), taskRequestDTO, taskCreator);

            //ASSERT
            verify(taskRepository, times(1)).save(taskCaptor.capture());
            Task savedTask = taskCaptor.getValue();

            assertNotNull(savedTask);
            assertEquals(ETaskStatus.TO_DO, savedTask.getStatus());
        }

        @Test
        @DisplayName("1.4 - shouldAutomaticallyAssignOwnerToTask_OnCreate")
        void shouldSetInitiashouldAutomaticallyAssignOwnerToTask_OnCreatelTaskStatusToToDo_OnCreate() {

            //ARRANGE
            when(projectService.findProjectEntityById(project.getId())).thenReturn(project);
            doNothing().when(projectAuthorizationService).checkProjectMembership(taskCreator, project);
            when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

            TaskResponseDTO taskResponseDTO = new TaskResponseDTO();
            taskResponseDTO.setId(1L);
            taskResponseDTO.setTitle(taskRequestDTO.getTitle());

            when(modelMapper.map(any(Task.class), eq(TaskResponseDTO.class))).thenReturn(taskResponseDTO);
            ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);

            //ACT
            TaskResponseDTO result = taskService.createTask(project.getId(), taskRequestDTO, taskCreator);

            //ASSERT
            verify(taskRepository, times(1)).save(taskCaptor.capture());
            Task savedTask = taskCaptor.getValue();

            assertNotNull(savedTask);
            assertEquals(taskCreator.getId(), savedTask.getAssignments().iterator().next().getUser().getId());
            assertEquals(taskCreator.getUsername(), savedTask.getAssignments().iterator().next().getUser().getUsername());
        }

        @Test
        @DisplayName("1.5 - shouldThrowAccessDeniedException_WhenUserIsNotProjectMember")
        void shouldThrowAccessDeniedException_WhenUserIsNotProjectMember() {

            //ARRANGE
            User nonMemberUser = TestDataFactory.createUserWithId(99L, "stranger", "stranger@example.com");

            when(projectService.findProjectEntityById(project.getId())).thenReturn(project);
            doThrow(new AccessDeniedException("Access denied. You are not a member of this project."))
                    .when(projectAuthorizationService)
                    .checkProjectMembership(nonMemberUser, project);

            //ACT & ASSERT
            assertThrows(AccessDeniedException.class, () -> taskService.createTask(project.getId(), taskRequestDTO, nonMemberUser));
            verify(taskRepository, never()).save(any(Task.class));
        }

        @Test
        @DisplayName("1.6 - shouldThrowResourceNotFoundException_WhenProjectDoesNotExist")
        void shouldThrowResourceNotFoundException_WhenProjectDoesNotExist() {

            //ARRANGE
            Long nonExistentProjectId = 999L;

            doThrow(new ResourceNotFoundException("Project not found")).
                    when(projectService)
                    .findProjectEntityById(nonExistentProjectId);

            //ACT & ASSERT
            assertThrows(ResourceNotFoundException.class, () -> taskService.createTask(nonExistentProjectId, taskRequestDTO, taskCreator));

            verify(projectService, times(1)).findProjectEntityById(nonExistentProjectId);
            verify(projectAuthorizationService, never()).checkProjectMembership(any(), any());
            verify(taskRepository, never()).save(any(Task.class));
        }
    }

    @Nested
    @DisplayName("2. Task State Transition Tests")
    class TaskStateTransitionTests {

        @Test
        @DisplayName("2.1 - shouldUpdateStatusToInProgress_WhenCurrentStatusIsToDo")
        void shouldUpdateStatusToInProgress_WhenCurrentStatusIsToDo() {

            //ARRANGE
            Task task = TestDataFactory.createMockTask(project, taskCreator, OffsetDateTime.now().plusDays(1));
            UpdateTaskStatusRequestDTO updateTaskDTO = TestDataFactory.createUpdateTaskStatusRequestDTO(ETaskStatus.IN_PROGRESS);

            when(taskProjectAssociationValidator.findAndValidate(task.getId(), project.getId())).thenReturn(task);
            doNothing().when(taskAuthorizationService).checkChangeStatusPermission(taskCreator, task);
            doNothing().when(taskStateTransitionValidator).validate(task.getStatus(), updateTaskDTO.getNewStatus());
            when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

            TaskUpdateStatusResponseDTO taskResponseDTO = new TaskUpdateStatusResponseDTO();
            taskResponseDTO.setId(1L);
            taskResponseDTO.setTitle(taskRequestDTO.getTitle());
            taskResponseDTO.setStatus(updateTaskDTO.getNewStatus());

            when(modelMapper.map(any(Task.class), eq(TaskUpdateStatusResponseDTO.class))).thenReturn(taskResponseDTO);
            ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);

            //ACT
            TaskUpdateStatusResponseDTO updatedTask = taskService.updateTaskStatus(project.getId(), task.getId(), updateTaskDTO, taskCreator);

            //ASSERT
            verify(taskRepository, times(1)).save(taskCaptor.capture());
            Task savedTask = taskCaptor.getValue();

            assertEquals(ETaskStatus.IN_PROGRESS, updatedTask.getStatus());
            assertEquals(updatedTask.getStatus(), savedTask.getStatus());
            verify(taskRepository, times(1)).save(task);
            verify(taskStateTransitionValidator, times(1)).validate(ETaskStatus.TO_DO, updateTaskDTO.getNewStatus());
        }

        @Test
        @DisplayName("2.2 - shouldUpdateStatusToDone_WhenCurrentStatusIsToDo")
        void shouldUpdateStatusToDone_WhenCurrentStatusIsToDo() {

            //ARRANGE
            Task task = TestDataFactory.createMockTask(project, taskCreator, OffsetDateTime.now().plusDays(1));
            UpdateTaskStatusRequestDTO updateTaskDTO = TestDataFactory.createUpdateTaskStatusRequestDTO(ETaskStatus.DONE, "Task done!");

            when(taskProjectAssociationValidator.findAndValidate(task.getId(), project.getId())).thenReturn(task);
            doNothing().when(taskAuthorizationService).checkChangeStatusPermission(taskCreator, task);
            doNothing().when(taskStateTransitionValidator).validate(task.getStatus(), updateTaskDTO.getNewStatus());
            when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

            TaskUpdateStatusResponseDTO taskResponseDTO = new TaskUpdateStatusResponseDTO();
            taskResponseDTO.setId(1L);
            taskResponseDTO.setTitle(taskRequestDTO.getTitle());
            taskResponseDTO.setStatus(updateTaskDTO.getNewStatus());
            taskResponseDTO.setCompletionComment(updateTaskDTO.getCompletionComment());

            when(modelMapper.map(any(Task.class), eq(TaskUpdateStatusResponseDTO.class))).thenReturn(taskResponseDTO);
            ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);

            //ACT
            TaskUpdateStatusResponseDTO updatedTask = taskService.updateTaskStatus(project.getId(), task.getId(), updateTaskDTO, taskCreator);

            //ASSERT
            verify(taskRepository, times(1)).save(taskCaptor.capture());
            Task savedTask = taskCaptor.getValue();

            assertEquals(ETaskStatus.DONE, updatedTask.getStatus());
            assertEquals(updatedTask.getStatus(), savedTask.getStatus());
            assertEquals(updatedTask.getCompletionComment(), updateTaskDTO.getCompletionComment());
            verify(taskRepository, times(1)).save(task);
            verify(taskStateTransitionValidator, times(1)).validate(ETaskStatus.TO_DO, updateTaskDTO.getNewStatus());
        }

        @Test
        @DisplayName("2.3 - shouldUpdateStatusToDone_WhenCurrentStatusIsInProgress")
        void shouldUpdateStatusToDone_WhenCurrentStatusIsInProgress() {

            //ARRANGE
            Task task = TestDataFactory.createMockTask(project, taskCreator, OffsetDateTime.now().plusDays(1));
            task.updateStatus(ETaskStatus.IN_PROGRESS);

            UpdateTaskStatusRequestDTO updateTaskDTO = TestDataFactory.createUpdateTaskStatusRequestDTO(ETaskStatus.DONE, "Task done!");

            when(taskProjectAssociationValidator.findAndValidate(task.getId(), project.getId())).thenReturn(task);
            doNothing().when(taskAuthorizationService).checkChangeStatusPermission(taskCreator, task);
            doNothing().when(taskStateTransitionValidator).validate(task.getStatus(), updateTaskDTO.getNewStatus());
            when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

            TaskUpdateStatusResponseDTO taskResponseDTO = new TaskUpdateStatusResponseDTO();
            taskResponseDTO.setId(1L);
            taskResponseDTO.setTitle(taskRequestDTO.getTitle());
            taskResponseDTO.setStatus(updateTaskDTO.getNewStatus());
            taskResponseDTO.setCompletionComment(updateTaskDTO.getCompletionComment());

            when(modelMapper.map(any(Task.class), eq(TaskUpdateStatusResponseDTO.class))).thenReturn(taskResponseDTO);
            ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);

            //ACT
            TaskUpdateStatusResponseDTO updatedTask = taskService.updateTaskStatus(project.getId(), task.getId(), updateTaskDTO, taskCreator);

            //ASSERT
            verify(taskRepository, times(1)).save(taskCaptor.capture());
            Task savedTask = taskCaptor.getValue();

            assertEquals(ETaskStatus.DONE, updatedTask.getStatus());
            assertEquals(updatedTask.getStatus(), savedTask.getStatus());
            assertEquals(updatedTask.getCompletionComment(), updateTaskDTO.getCompletionComment());
            verify(taskRepository, times(1)).save(task);
            verify(taskStateTransitionValidator, times(1)).validate(ETaskStatus.IN_PROGRESS, updateTaskDTO.getNewStatus());
        }

        @Test
        @DisplayName("2.4 - shouldSetCompletionDate_WhenStatusChangesToDone")
        void shouldSetCompletionDate_WhenStatusChangesToDone() {

            //ARRANGE
            Task task = TestDataFactory.createMockTask(project, taskCreator, OffsetDateTime.now().plusDays(1));
            assertNull(task.getCompletedAt());

            UpdateTaskStatusRequestDTO updateTaskDTO = TestDataFactory.createUpdateTaskStatusRequestDTO(ETaskStatus.DONE, "Task done!");

            when(taskProjectAssociationValidator.findAndValidate(task.getId(), project.getId())).thenReturn(task);
            doNothing().when(taskAuthorizationService).checkChangeStatusPermission(taskCreator, task);
            doNothing().when(taskStateTransitionValidator).validate(task.getStatus(), updateTaskDTO.getNewStatus());
            when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

            TaskUpdateStatusResponseDTO taskResponseDTO = new TaskUpdateStatusResponseDTO();
            taskResponseDTO.setId(1L);
            taskResponseDTO.setTitle(taskRequestDTO.getTitle());
            taskResponseDTO.setStatus(updateTaskDTO.getNewStatus());
            taskResponseDTO.setCompletionComment(updateTaskDTO.getCompletionComment());
            taskResponseDTO.setCompletedAt(OffsetDateTime.now());

            when(modelMapper.map(any(Task.class), eq(TaskUpdateStatusResponseDTO.class))).thenReturn(taskResponseDTO);
            ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);

            //ACT
            TaskUpdateStatusResponseDTO updatedTask = taskService.updateTaskStatus(project.getId(), task.getId(), updateTaskDTO, taskCreator);

            //ASSERT
            verify(taskRepository, times(1)).save(taskCaptor.capture());
            Task savedTask = taskCaptor.getValue();

            assertNotNull(updatedTask.getCompletedAt());
            assertTrue(savedTask.getCompletedAt().isAfter(OffsetDateTime.now().minusSeconds(5)));
        }

        @Test
        @DisplayName("2.5 - shouldThrowInvalidTaskStateException_WhenTransitioningFromDoneToAnyStatus")
        void shouldThrowInvalidTaskStateException_WhenTransitioningFromDoneToAnyStatus() {

            //ARRANGE
            Task task = TestDataFactory.createMockTask(project, taskCreator, OffsetDateTime.now().plusDays(1));
            task.updateStatus(ETaskStatus.DONE);

            UpdateTaskStatusRequestDTO updateTaskDTO = TestDataFactory.createUpdateTaskStatusRequestDTO(ETaskStatus.TO_DO);

            when(taskProjectAssociationValidator.findAndValidate(task.getId(), project.getId())).thenReturn(task);
            doNothing().when(taskAuthorizationService).checkChangeStatusPermission(taskCreator, task);
            doThrow(new InvalidTaskStateException("Invalid Status Task."))
                    .when(taskStateTransitionValidator)
                    .validate(task.getStatus(), updateTaskDTO.getNewStatus());

            //ACT & ASSERT

            assertThrows(InvalidTaskStateException.class, () -> taskService.updateTaskStatus(project.getId(), task.getId(), updateTaskDTO, taskCreator));
            verify(taskRepository, never()).save(any(Task.class));
        }

        @Test
        @DisplayName("2.6 - shouldThrowInvalidTaskStateException_WhenTransitioningFromInProgressToToDo")
        void shouldThrowInvalidTaskStateException_WhenTransitioningFromInProgressToToDo() {

            //ARRANGE
            Task task = TestDataFactory.createMockTask(project, taskCreator, OffsetDateTime.now().plusDays(1));
            task.updateStatus(ETaskStatus.IN_PROGRESS);

            UpdateTaskStatusRequestDTO updateTaskDTO = TestDataFactory.createUpdateTaskStatusRequestDTO(ETaskStatus.TO_DO);

            when(taskProjectAssociationValidator.findAndValidate(task.getId(), project.getId())).thenReturn(task);
            doNothing().when(taskAuthorizationService).checkChangeStatusPermission(taskCreator, task);
            doThrow(new InvalidTaskStateException("Invalid Status Task."))
                    .when(taskStateTransitionValidator)
                    .validate(task.getStatus(), updateTaskDTO.getNewStatus());

            //ACT & ASSERT

            assertThrows(InvalidTaskStateException.class, () -> taskService.updateTaskStatus(project.getId(), task.getId(), updateTaskDTO, taskCreator));
            verify(taskRepository, never()).save(any(Task.class));
        }

        @Test
        @DisplayName("2.7 - shouldThrowInvalidTaskStateException_WhenNewStatusIsSameAsCurrent")
        void shouldThrowInvalidTaskStateException_WhenNewStatusIsSameAsCurrent() {

            //ARRANGE
            Task task = TestDataFactory.createMockTask(project, taskCreator, OffsetDateTime.now().plusDays(1));

            UpdateTaskStatusRequestDTO updateTaskDTO = TestDataFactory.createUpdateTaskStatusRequestDTO(ETaskStatus.TO_DO);

            when(taskProjectAssociationValidator.findAndValidate(task.getId(), project.getId())).thenReturn(task);
            doNothing().when(taskAuthorizationService).checkChangeStatusPermission(taskCreator, task);
            doThrow(new InvalidTaskStateException("Invalid Status Task."))
                    .when(taskStateTransitionValidator)
                    .validate(task.getStatus(), updateTaskDTO.getNewStatus());

            //ACT & ASSERT

            assertThrows(InvalidTaskStateException.class, () -> taskService.updateTaskStatus(project.getId(), task.getId(), updateTaskDTO, taskCreator));
            verify(taskRepository, never()).save(any(Task.class));
        }

        @Test
        @DisplayName("2.8 - shouldThrowAccessDeniedException_WhenUserLacksChangeStatusPermission")
        void shouldThrowAccessDeniedException_WhenUserLacksChangeStatusPermission() {

            //ARRANGE
            Task task = TestDataFactory.createMockTask(project, taskCreator, OffsetDateTime.now().plusDays(1));

            UpdateTaskStatusRequestDTO updateTaskDTO = TestDataFactory.createUpdateTaskStatusRequestDTO(ETaskStatus.IN_PROGRESS);

            when(taskProjectAssociationValidator.findAndValidate(task.getId(), project.getId())).thenReturn(task);
            doThrow(new AccessDeniedException("Permission denied."))
                    .when(taskAuthorizationService)
                    .checkChangeStatusPermission(taskCreator, task);

            //ACT & ASSERT

            assertThrows(AccessDeniedException.class, () -> taskService.updateTaskStatus(project.getId(), task.getId(), updateTaskDTO, taskCreator));
            verify(taskRepository, never()).save(any(Task.class));
        }
    }

    @Nested
    @DisplayName("3. Task Assignments Tests")
    class taskAssignmentsTests {

        @Test
        @DisplayName("3.1 - shouldAssignUserToTaskSuccessfully_WhenUserIsProjectMember")
        void shouldAssignUserToTaskSuccessfully_WhenUserIsProjectMember() {

            //ARRANGE
            Task task = TestDataFactory.createMockTask(project, taskCreator, OffsetDateTime.now().plusDays(1));

            Set<TaskAssignmentRequestDTO> assignmentsUsersDTO = TestDataFactory.createTaskAssignmentRequestDTOSet();
            List<Long> usersIds = assignmentsUsersDTO.stream().map(a -> a.getUserId()).toList();
            Set<User> assingmentsUsers= TestDataFactory.createAssignmentusers(usersIds);
            Set<UserAssignmentResponseDTO> assignmentsUserDTO = TestDataFactory.createAssignmentusersDTO(assingmentsUsers);

            when(taskProjectAssociationValidator.findAndValidate(task.getId(), project.getId())).thenReturn(task);
            doNothing().when(taskAuthorizationService).checkEditPermission(taskCreator, task);
            doNothing().when(taskAssignmentRoleValidator).validate(assignmentsUsersDTO);
            when(projectService.findProjectMembersFromIdList(project.getId(), usersIds)).thenReturn(assingmentsUsers);
            when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

            TaskResponseDTO taskResponseDTO = new TaskResponseDTO();
            taskResponseDTO.setId(1L);
            taskResponseDTO.setAssignments(assignmentsUserDTO);

            when(modelMapper.map(any(Task.class), eq(TaskResponseDTO.class))).thenReturn(taskResponseDTO);
            ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);

            //ACT
            TaskResponseDTO updatedTask = taskService.assignUsersToTask(project.getId(), task.getId(), assignmentsUsersDTO, taskCreator);

            //ASSERT
            verify(taskRepository, times(1)).save(taskCaptor.capture());
            Task savedTask = taskCaptor.getValue();

            assertNotNull(savedTask);
            assertEquals(2, savedTask.getAssignments().size());
        }
    }
}

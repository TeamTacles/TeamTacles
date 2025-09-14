package br.com.teamtacles.config.aop;

import br.com.teamtacles.project.dto.response.ProjectResponseDTO;
import br.com.teamtacles.team.dto.request.InvitedMemberRequestDTO;
import br.com.teamtacles.team.dto.request.TeamRequestRegisterDTO;
import br.com.teamtacles.team.dto.request.UpdateMemberRoleTeamRequestDTO;
import br.com.teamtacles.user.dto.request.UserRequestRegisterDTO;
import br.com.teamtacles.user.dto.response.UserResponseDTO;
import br.com.teamtacles.user.model.User;
import br.com.teamtacles.project.dto.request.InviteProjectMemberRequestDTO;
import br.com.teamtacles.project.dto.request.ProjectRequestRegisterDTO;
import br.com.teamtacles.project.dto.request.UpdateMemberRoleProjectRequestDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.util.Arrays;
import br.com.teamtacles.team.dto.response.TeamResponseDTO;


@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    //  POINTCUTS GERAIS

    @Pointcut("within(br.com.teamtacles.*.controller..*)")
    public void controllerPackagePointcut() {}


    @Pointcut("execution(public * br.com.teamtacles.*.service..*(..))")
    public void servicePackagePointcut() {}

    //  POINTCUTS  TEAMSERVICE

    @Pointcut("execution(* br.com.teamtacles.team.service.TeamService.createTeam(..)) && args(dto, actingUser)")
    public void teamCreationPointcut(TeamRequestRegisterDTO dto, User actingUser) {}

    @Pointcut("execution(* br.com.teamtacles.team.service.TeamService.inviteMember(..)) && args(teamId, dto, actingUser)")
    public void teamInvitationPointcut(Long teamId, InvitedMemberRequestDTO dto, User actingUser) {}

    @Pointcut("execution(* br.com.teamtacles.team.service.TeamService.acceptInvitation(..)) && args(token)")
    public void teamAcceptInvitationPointcut(String token) {}

    @Pointcut("execution(* br.com.teamtacles.team.service.TeamService.updateTeam(..)) && args(teamId, *, actingUser)")
    public void teamUpdatePointcut(Long teamId, User actingUser) {}

    @Pointcut("execution(* br.com.teamtacles.team.service.TeamService.updateMemberRole(..)) && args(teamId, userIdToUpdate, dto, actingUser)")
    public void teamUpdateMemberRolePointcut(Long teamId, Long userIdToUpdate, UpdateMemberRoleTeamRequestDTO dto, User actingUser) {}

    @Pointcut("execution(* br.com.teamtacles.team.service.TeamService.deleteTeam(..)) && args(teamId, actingUser)")
    public void teamDeletionPointcut(Long teamId, User actingUser) {}

    @Pointcut("execution(* br.com.teamtacles.team.service.TeamService.deleteMembershipFromTeam(..)) && args(teamId, userIdToDelete, actingUser)")
    public void teamDeleteMembershipPointcut(Long teamId, Long userIdToDelete, User actingUser) {}

    //  POINTCUTS USER SERVICE

    @Pointcut("execution(* br.com.teamtacles.user.service.UserService.createUser(..)) && args(userDto)")
    public void userCreationPointcut(UserRequestRegisterDTO userDto) {}

    @Pointcut("execution(* br.com.teamtacles.user.service.UserService.updateUser(..)) && args(*, user)")
    public void userUpdatePointcut(User user) {}

    @Pointcut("execution(* br.com.teamtacles.user.service.UserService.deleteUser(..)) && args(user)")
    public void userDeletionPointcut(User user) {}

    @Pointcut("execution(* br.com.teamtacles.user.service.UserService.resetPassword(..)) && args(token, *, *)")
    public void userResetPasswordPointcut(String token) {}

    @Pointcut("execution(* br.com.teamtacles.user.service.UserService.verifyUser(..)) && args(token)")
    public void userVerificationPointcut(String token) {}

    //   POINTCUTS PROJECTSERVICE

    @Pointcut("execution(* br.com.teamtacles.project.service.ProjectService.createProject(..)) && args(dto, actingUser)")
    public void projectCreationPointcut(ProjectRequestRegisterDTO dto, User actingUser) {}

    @Pointcut("execution(* br.com.teamtacles.project.service.ProjectService.updateProject(..)) && args(projectId, *, actingUser)")
    public void projectUpdatePointcut(Long projectId, User actingUser) {}

    @Pointcut("execution(* br.com.teamtacles.project.service.ProjectService.deleteProject(..)) && args(projectId, actingUser)")
    public void projectDeletionPointcut(Long projectId, User actingUser) {}

    @Pointcut("execution(* br.com.teamtacles.project.service.ProjectService.inviteMember(..)) && args(projectId, dto, actingUser)")
    public void projectInvitationPointcut(Long projectId, InviteProjectMemberRequestDTO dto, User actingUser) {}

    @Pointcut("execution(* br.com.teamtacles.project.service.ProjectService.importTeamMembersToProject(..)) && args(projectId, teamId, actingUser)")
    public void projectImportTeamPointcut(Long projectId, Long teamId, User actingUser) {}



    //  ADVICES

    @Before("controllerPackagePointcut()")
    public void logBeforeRequest(JoinPoint joinPoint) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            log.info("==> Request [{} {}] | Controller [{}::{}]",
                    request.getMethod(), request.getRequestURI(),
                    joinPoint.getSignature().getDeclaringTypeName(), joinPoint.getSignature().getName());
        }
    }

    //  ADVICES TEAMSERVICE

    @Before("teamCreationPointcut(dto, actingUser)")
    public void logTeamCreation(TeamRequestRegisterDTO dto, User actingUser) {
        log.info("[ACTION] User '{}' is attempting to create team '{}'", actingUser.getUsername(), dto.getName());
    }

    @AfterReturning(pointcut = "teamCreationPointcut(dto, actingUser)", returning = "result")
    public void logTeamCreationSuccess(TeamRequestRegisterDTO dto, User actingUser, TeamResponseDTO result) {
        if (result != null) {
            log.info("[SUCCESS] Team '{}' with ID [{}] created successfully by user '{}'",
                    result.getName(), result.getId(), actingUser.getUsername());
        }
    }

    @Before("teamInvitationPointcut(teamId, dto, actingUser)")
    public void logTeamInvitation(Long teamId, InvitedMemberRequestDTO dto, User actingUser) {
        log.info("[ACTION] User '{}' is inviting user with email '{}' to team ID [{}] with role {}",
                actingUser.getUsername(), dto.getEmail(), teamId, dto.getRole());
    }

    @AfterReturning("teamInvitationPointcut(teamId, dto, actingUser)")
    public void logTeamInvitationSuccess(Long teamId, InvitedMemberRequestDTO dto, User actingUser) {
        log.info("[SUCCESS] Invitation successfully sent to '{}' for team ID [{}].",
                dto.getEmail(), teamId);
    }

    @Before("teamAcceptInvitationPointcut(token)")
    public void logAcceptInvitation(String token) {
        log.info("[ACTION] Processing invitation acceptance with a token.");
    }

    @AfterReturning("teamAcceptInvitationPointcut(token)")
    public void logAcceptInvitationSuccess(String token) {
        log.info("[SUCCESS] Invitation accepted successfully.");
    }

    @Before("teamUpdatePointcut(teamId, actingUser)")
    public void logTeamUpdate(Long teamId, User actingUser) {
        log.info("[ACTION] User '{}' is attempting to update team ID [{}]", actingUser.getUsername(), teamId);
    }

    @AfterReturning(pointcut = "teamUpdatePointcut(teamId, actingUser)", returning = "result")
    public void logTeamUpdateSuccess(Long teamId, User actingUser, TeamResponseDTO result) {
        if (result != null) {
            log.info("[SUCCESS] Team '{}' (ID: [{}]) was updated successfully by user '{}'.",
                    result.getName(), teamId, actingUser.getUsername());
        }
    }

    @Before("teamUpdateMemberRolePointcut(teamId, userIdToUpdate, dto, actingUser)")
    public void logUpdateMemberRole(Long teamId, Long userIdToUpdate, UpdateMemberRoleTeamRequestDTO dto, User actingUser) {
        log.info("[ACTION] User '{}' is attempting to update role for user ID [{}] in team ID [{}]. New role: {}",
                actingUser.getUsername(), userIdToUpdate, teamId, dto.getNewRole());
    }

    @AfterReturning("teamUpdateMemberRolePointcut(teamId, userIdToUpdate, dto, actingUser)")
    public void logUpdateMemberRoleSuccess(Long teamId, Long userIdToUpdate, UpdateMemberRoleTeamRequestDTO dto, User actingUser) {
        log.info("[SUCCESS] Role for user ID [{}] in team ID [{}] was updated to {} successfully.",
                userIdToUpdate, teamId, dto.getNewRole());
    }

    @Before("teamDeletionPointcut(teamId, actingUser)")
    public void logTeamDeletion(Long teamId, User actingUser) {
        log.info("[ACTION] User '{}' is attempting to delete team ID [{}]", actingUser.getUsername(), teamId);
    }

    @AfterReturning("teamDeletionPointcut(teamId, actingUser)")
    public void logTeamDeletionSuccess(Long teamId, User actingUser) {
        log.info("[SUCCESS] Team ID [{}] was deleted successfully by user '{}'.",
                teamId, actingUser.getUsername());
    }

    @Before("teamDeleteMembershipPointcut(teamId, userIdToDelete, actingUser)")
    public void logDeleteMembership(Long teamId, Long userIdToDelete, User actingUser) {
        log.info("[ACTION] User '{}' is attempting to remove user ID [{}] from team ID [{}]",
                actingUser.getUsername(), userIdToDelete, teamId);
    }

    @AfterReturning("teamDeleteMembershipPointcut(teamId, userIdToDelete, actingUser)")
    public void logDeleteMembershipSuccess(Long teamId, Long userIdToDelete, User actingUser) {
        log.info("[SUCCESS] User ID [{}] was successfully removed from team ID [{}] by user '{}'.",
                userIdToDelete, teamId, actingUser.getUsername());
    }

    //  ADVICES USERSERVICE
    @Before("userCreationPointcut(userDto)")
    public void logUserCreation(UserRequestRegisterDTO userDto) {
        log.info("[ACTION] Attempting to create a new user with username: {}", userDto.getUsername());
    }

    @AfterReturning(pointcut = "userCreationPointcut(userDto)", returning = "result")
    public void logUserCreationSucess(UserRequestRegisterDTO userDto, UserResponseDTO result) {
        if(result != null) {
            log.info("[SUCCESS] User created successfully with ID: {} and username: {}", result.getId(), result.getUsername());
        }
    }

    @Before("userUpdatePointcut(user)")
    public void logUserUpdate(User user) {
        log.info("[ACTION] Attempting to update user with ID: {}", user.getId());
    }

    @AfterReturning(pointcut = "userUpdatePointcut(user)", returning = "result")
    public void logUserUpdateSuccess(User user, UserResponseDTO result) {
        if (result != null) {
            log.info("[SUCCESS] Profile for user '{}' (ID: {}) was updated successfully.",
                    result.getUsername(), result.getId());
        }
    }


    @Before("userDeletionPointcut(user)")
    public void logUserDeletion(User user) {
        log.info("[ACTION] Attempting to delete user with ID: {}", user.getId());
    }

    @AfterReturning("userDeletionPointcut(user)")
    public void logUserDeletionSuccess(User user) {
        log.info("[SUCCESS] User with ID: {} was deleted successfully.", user.getId());
    }

    @Before("userResetPasswordPointcut(token)")
    public void logUserResetPassword(String token) {
        log.info("[ACTION] Processing password reset request with a token.");
    }

    @AfterReturning("userResetPasswordPointcut(token)")
    public void logUserResetPasswordSuccess(String token) {
        log.info("[SUCCESS] Password was reset successfully.");
    }


    @Before("userVerificationPointcut(token)")
    public void logUserVerification(String token) {
        log.info("[ACTION] Processing user verification with a token.");
    }

    @AfterReturning("userVerificationPointcut(token)")
    public void logUserVerificationSuccess(String token) {
        log.info("[SUCCESS] User was verified successfully.");
    }

    //  ADVICES PROJECTSERVICE

    @Before("projectCreationPointcut(dto, actingUser)")
    public void logProjectCreation(ProjectRequestRegisterDTO dto, User actingUser) {
        log.info("[ACTION] User '{}' is attempting to create project '{}'", actingUser.getUsername(), dto.getTitle());
    }

    @AfterReturning(pointcut = "projectCreationPointcut(dto, actingUser)", returning = "result")
    public void logProjectCreationSuccess(ProjectRequestRegisterDTO dto, User actingUser, ProjectResponseDTO result) {
        if (result != null) {
            log.info("[SUCCESS] Project '{}' with ID [{}] created successfully by user '{}'",
                    result.getTitle(), result.getId(), actingUser.getUsername());
        }
    }

    @Before("projectUpdatePointcut(projectId, actingUser)")
    public void logProjectUpdate(Long projectId, User actingUser) {
        log.info("[ACTION] User '{}' is attempting to update project ID [{}]", actingUser.getUsername(), projectId);
    }

    @AfterReturning(pointcut = "projectUpdatePointcut(projectId, actingUser)", returning = "result")
    public void logProjectUpdateSuccess(Long projectId, User actingUser, ProjectResponseDTO result) {
        if (result != null) {
            log.info("[SUCCESS] Project '{}' (ID: [{}]) was updated successfully by user '{}'.",
                    result.getTitle(), projectId, actingUser.getUsername());
        }
    }

    @Before("projectDeletionPointcut(projectId, actingUser)")
    public void logProjectDeletion(Long projectId, User actingUser) {
        log.info("[ACTION] User '{}' is attempting to delete project ID [{}]", actingUser.getUsername(), projectId);
    }

    @AfterReturning("projectDeletionPointcut(projectId, actingUser)")
    public void logProjectDeletionSuccess(Long projectId, User actingUser) {
        log.info("[SUCCESS] Project ID [{}] was deleted successfully by user '{}'.", projectId, actingUser.getUsername());
    }

    @Before("projectInvitationPointcut(projectId, dto, actingUser)")
    public void logProjectInvitation(Long projectId, InviteProjectMemberRequestDTO dto, User actingUser) {
        log.info("[ACTION] User '{}' is inviting user with email '{}' to project ID [{}] with role {}",
                actingUser.getUsername(), dto.getEmail(), projectId, dto.getRole());
    }

    @AfterReturning("projectInvitationPointcut(projectId, dto, actingUser)")
    public void logProjectInvitationSuccess(Long projectId, InviteProjectMemberRequestDTO dto, User actingUser) {
        log.info("[SUCCESS] Invitation successfully sent to '{}' for project ID [{}].",
                dto.getEmail(), projectId);
    }

    @Before("projectImportTeamPointcut(projectId, teamId, actingUser)")
    public void logProjectImport(Long projectId, Long teamId, User actingUser) {
        log.info("[ACTION] User '{}' is importing members from team ID [{}] to project ID [{}]",
                actingUser.getUsername(), teamId, projectId);
    }

    @AfterReturning("projectImportTeamPointcut(projectId, teamId, actingUser)")
    public void logProjectImportSuccess(Long projectId, Long teamId, User actingUser) {
        log.info("[SUCCESS] Members from team ID [{}] were imported successfully to project ID [{}].",
                teamId, projectId);
    }

    //  ADVICES GENÉRICO

    @Around("servicePackagePointcut()")
    public Object logServiceMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        log.debug("==> Entering Service [{}::{}] | Arguments: {}", className, methodName, Arrays.toString(joinPoint.getArgs()));

        long startTime = System.currentTimeMillis();
        Object result;

        try {
            result = joinPoint.proceed();
            long timeTaken = System.currentTimeMillis() - startTime;
            log.debug("<== Exiting Service [{}::{}] | Result: {} | Duration: {}ms", className, methodName, result, timeTaken);
            return result;
        } catch (Throwable throwable) {
            long timeTaken = System.currentTimeMillis() - startTime;
            log.error("<== Exception in Service [{}::{}] | Exception: {} | Duration: {}ms", className, methodName, throwable.getMessage(), timeTaken);
            throw throwable;
        }
    }
}
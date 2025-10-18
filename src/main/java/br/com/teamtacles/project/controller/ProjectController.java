package br.com.teamtacles.project.controller;

import br.com.teamtacles.common.dto.response.MessageResponseDTO;
import br.com.teamtacles.common.dto.response.page.PagedResponse;
import br.com.teamtacles.common.exception.ErrorResponse;
import br.com.teamtacles.orchestration.service.UserAccountService;
import br.com.teamtacles.project.dto.response.PdfExportResult;
import br.com.teamtacles.project.dto.response.ProjectReportDTO;
import br.com.teamtacles.project.dto.request.*;
import br.com.teamtacles.project.dto.response.ProjectMemberResponseDTO;
import br.com.teamtacles.project.dto.response.ProjectResponseDTO;
import br.com.teamtacles.project.dto.response.UserProjectResponseDTO;
import br.com.teamtacles.infrastructure.export.ProjectPdfExportService;
import br.com.teamtacles.project.service.ProjectService;
import br.com.teamtacles.security.UserAuthenticated;
import br.com.teamtacles.common.dto.response.InviteLinkResponseDTO;
import br.com.teamtacles.task.dto.request.TaskFilterReportDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/project")
@Tag(name = "Project Management", description = "Endpoints for creating, managing, and interacting with projects.")
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectPdfExportService projectPdfExportService;
    private final UserAccountService userAccountService;

    public ProjectController(ProjectService projectService, ProjectPdfExportService projectPdfExportService, UserAccountService userAccountService) {
        this.projectService = projectService;
        this.projectPdfExportService = projectPdfExportService;
        this.userAccountService = userAccountService;
    }


    @Operation(summary = "Create a new project", description = "Creates a new project and sets the authenticated user as the owner.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Project created successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProjectResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "A project with this title already exists for the user",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<ProjectResponseDTO> createProject(
            @RequestBody @Valid ProjectRequestRegisterDTO requestDTO,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        ProjectResponseDTO responseDTO = projectService.createProject(requestDTO, authenticatedUser.getUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    }

    @Operation(summary = "Invite a user to a project via email", description = "Sends an email invitation to a user to join a project. Requires ADMIN or OWNER role.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Invitation email sent successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden, user lacks permission to invite members",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project or user to be invited not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Conflict, the user is already a member of this project",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{projectId}/invite-email")
    public ResponseEntity<Void> inviteMember(
            @PathVariable Long projectId,
            @RequestBody @Valid InviteProjectMemberRequestDTO requestDTO,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        projectService.inviteMemberByEmail(projectId, requestDTO, authenticatedUser.getUser());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Generate a project invitation link", description = "Generates a shareable link to invite users to a project. Requires ADMIN or OWNER role.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Invitation link generated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = InviteLinkResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden, user lacks permission to generate links",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{projectId}/invite-link")
    public ResponseEntity<InviteLinkResponseDTO> generateInvitedLink(
            @PathVariable Long projectId,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        InviteLinkResponseDTO inviteLinkDTO = projectService.generateInvitedLink(projectId, authenticatedUser.getUser());
        return ResponseEntity.ok(inviteLinkDTO);
    }

    @Operation(summary = "Join a project using an invitation link", description = "Allows the authenticated user to join a project using a valid invitation token from a link.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully joined the project",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProjectMemberResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Invitation token is invalid or expired",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Conflict, the user is already a member of this project",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/join")
    public ResponseEntity<ProjectMemberResponseDTO> joinProjectWithLink(
            @RequestParam String token,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        ProjectMemberResponseDTO projectMemberDTO = projectService.acceptInvitationFromLink(token, authenticatedUser.getUser());
        return ResponseEntity.ok(projectMemberDTO);
    }

    @Operation(summary = "Import team members to a project", description = "Imports all members of a specified team into a project. Requires project ADMIN or OWNER role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Team members imported successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden, user lacks permission to import members",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project or Team not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{projectId}/import-team/{teamId}")
    public ResponseEntity<Void> importTeamToProject(
            @PathVariable Long projectId,
            @PathVariable Long teamId,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        projectService.importTeamMembersToProject(projectId, teamId, authenticatedUser.getUser());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update a project member's role", description = "Updates the role of a member within a project. Requires ADMIN or OWNER role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Member's role updated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProjectMemberResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden, user lacks permission to update member roles",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project or member not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{projectId}/member/{userId}/role")
    public ResponseEntity<ProjectMemberResponseDTO> updateMemberRole(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            @RequestBody @Valid UpdateMemberRoleProjectRequestDTO dto,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        ProjectMemberResponseDTO updatedMember = projectService.updateMemberRole(projectId, userId, dto, authenticatedUser.getUser());
        return ResponseEntity.ok(updatedMember);
    }

    @Operation(summary = "Get a project by ID", description = "Retrieves the details of a specific project by its ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Project found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProjectResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden, user is not a member of the project",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectResponseDTO> getProjectById(
            @PathVariable Long projectId,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        ProjectResponseDTO projectDTO = projectService.getProjectById(projectId, authenticatedUser.getUser());
        return ResponseEntity.ok(projectDTO);
    }

    @Operation(summary = "List projects for the authenticated user", description = "Retrieves a paginated list of projects associated with the authenticated user, with optional filtering.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User's projects retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<PagedResponse<UserProjectResponseDTO>> getAllProjectsByUser(
            @ModelAttribute ProjectFilterDTO filter,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser,
            Pageable pageable) {
        PagedResponse<UserProjectResponseDTO> projects = projectService.getAllProjectsByUser(pageable, filter, authenticatedUser.getUser());
        return ResponseEntity.ok(projects);
    }

    @Operation(summary = "List members of a project", description = "Retrieves a paginated list of all members in a specific project.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Members retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden, user is not a member of the project",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{projectId}/members")
    public ResponseEntity<PagedResponse<ProjectMemberResponseDTO>> getAllMembersFromProject(
            @PathVariable Long projectId,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser,
            Pageable pageable) {
        PagedResponse<ProjectMemberResponseDTO> usersFromProject = projectService.getAllMembersFromProject(pageable, projectId, authenticatedUser.getUser());
        return ResponseEntity.ok(usersFromProject);
    }

    @Operation(summary = "Export a project report to PDF", description = "Exports the project report to a PDF file.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "PDF exported successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE)),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden, user is not a member of the project",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error during PDF generation",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping(value = "/{projectId}/export/pdf")
    public ResponseEntity<byte[]> exportProjectToPdf(
            @PathVariable Long projectId,
            @ModelAttribute TaskFilterReportDTO filter,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {

        PdfExportResult pdfReport = projectPdfExportService.generateProjectPdf(projectId, authenticatedUser.getUser(), filter);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);

        headers.setContentDispositionFormData("attachment", pdfReport.getFilename());

        return ResponseEntity.ok().headers(headers).body(pdfReport.getContent());
    }

    @Operation(summary = "Get project dashboard", description = "Generates a comprehensive report for a project, including task summaries and member performance.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dashboard report generated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProjectReportDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden, user is not a member of the project",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{projectId}/dashboard")
    public ResponseEntity<ProjectReportDTO> getProjectDashboard(
            @PathVariable Long projectId,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser
    ) {
        ProjectReportDTO report = projectService.getProjectReport(projectId, authenticatedUser.getUser());
        return ResponseEntity.ok(report);
    }

    @Operation(summary = "Update a project", description = "Updates the details of an existing project. Requires ADMIN or OWNER role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Project updated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProjectResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden, user lacks permission to update the project",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "A project with this title already exists for the user",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{projectId}")
    public ResponseEntity<ProjectResponseDTO> updateProject(
            @PathVariable Long projectId,
            @RequestBody @Valid ProjectRequestUpdateDTO requestDTO,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        ProjectResponseDTO updatedProject = projectService.updateProject(projectId, requestDTO, authenticatedUser.getUser());
        return ResponseEntity.ok(updatedProject);
    }

    @Operation(summary = "Delete a project", description = "Deletes a project by its ID. Requires OWNER role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Project deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden, user lacks permission to delete the project",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(
            @PathVariable Long projectId,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        projectService.deleteProject(projectId, authenticatedUser.getUser());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Remove a member from a project", description = "Removes a member from a project. Requires ADMIN or OWNER role. Owners cannot be removed.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Member removed successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden, user lacks permission to remove members",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project or member not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{projectId}/member/{userId}")
    public ResponseEntity<Void> deleteMembershipFromProject(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        projectService.deleteMembershipFromProject(projectId, userId, authenticatedUser.getUser());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Leave a project", description = "Allows an authenticated user to leave a project they are a member of.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Successfully left the project"),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden, the user is the owner and cannot leave the project",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "project or membership not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{projectId}/leave")
    public ResponseEntity<Void> leaveProject(
            @PathVariable Long projectId,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        userAccountService.leaveProjectAndTasks(projectId, authenticatedUser.getUser());
        return ResponseEntity.noContent().build();
    }
}

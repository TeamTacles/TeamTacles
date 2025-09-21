package br.com.teamtacles.project.controller;

import br.com.teamtacles.common.dto.response.MessageResponseDTO;
import br.com.teamtacles.common.dto.response.page.PagedResponse;
import br.com.teamtacles.common.util.ReportFileNameGenerator;
import br.com.teamtacles.project.dto.report.TaskSummary;
import br.com.teamtacles.project.dto.request.*;
import br.com.teamtacles.project.dto.response.DashboardSummaryDTO;
import br.com.teamtacles.project.dto.response.ProjectMemberResponseDTO;
import br.com.teamtacles.project.dto.response.ProjectResponseDTO;
import br.com.teamtacles.project.dto.response.UserProjectResponseDTO;
import br.com.teamtacles.project.model.Project;
import br.com.teamtacles.project.service.PdfExportProjectService;
import br.com.teamtacles.project.service.ProjectService;
import br.com.teamtacles.security.UserAuthenticated;
import br.com.teamtacles.common.dto.response.InviteLinkResponseDTO;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@RestController
@RequestMapping("/api/project")
public class ProjectController {

    private final ProjectService projectService;
    private final PdfExportProjectService pdfExportProjectService;

    public ProjectController(ProjectService projectService, PdfExportProjectService pdfExportProjectService) {
        this.projectService = projectService;
        this.pdfExportProjectService = pdfExportProjectService;
    }

    @PostMapping
    public ResponseEntity<ProjectResponseDTO> createProject(
            @RequestBody @Valid ProjectRequestRegisterDTO requestDTO,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        ProjectResponseDTO responseDTO = projectService.createProject(requestDTO, authenticatedUser.getUser());
        return ResponseEntity.ok(responseDTO);
    }

    @PostMapping("/{projectId}/invite")
    public ResponseEntity<Void> inviteMember(
            @PathVariable Long projectId,
            @RequestBody @Valid InviteProjectMemberRequestDTO requestDTO,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        projectService.inviteMember(projectId, requestDTO, authenticatedUser.getUser());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/{projectId}/invite-link")
    public ResponseEntity<InviteLinkResponseDTO> generateInvitedLink(
            @PathVariable Long projectId,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        InviteLinkResponseDTO inviteLinkDTO = projectService.generateInvitedLink(projectId, authenticatedUser.getUser());
        return ResponseEntity.ok(inviteLinkDTO);
    }

    @PostMapping("/join")
    public ResponseEntity<ProjectMemberResponseDTO> joinProjectWithLink(
            @RequestParam String token,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        ProjectMemberResponseDTO projectMemberDTO = projectService.acceptProjectInvitationLink(token, authenticatedUser.getUser());
        return ResponseEntity.ok(projectMemberDTO);
    }

    @PostMapping("/{projectId}/import-team/{teamId}")
    public ResponseEntity<Void> importTeamToProject(
            @PathVariable Long projectId,
            @PathVariable Long teamId,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        projectService.importTeamMembersToProject(projectId, teamId, authenticatedUser.getUser());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{projectId}/member/{userId}/role")
    public ResponseEntity<ProjectMemberResponseDTO> updateMemberRole(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            @RequestBody @Valid UpdateMemberRoleProjectRequestDTO dto,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        ProjectMemberResponseDTO updatedMember = projectService.updateMemberRole(projectId, userId, dto, authenticatedUser.getUser());
        return ResponseEntity.ok(updatedMember);
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectResponseDTO> getProjectById(
            @PathVariable Long projectId,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        ProjectResponseDTO projectDTO = projectService.getProjectById(projectId, authenticatedUser.getUser());
        return ResponseEntity.ok(projectDTO);
    }

    @GetMapping
    public ResponseEntity<PagedResponse<UserProjectResponseDTO>> getAllProjectsByUser(
            @ModelAttribute ProjectFilterDTO filter,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser,
            Pageable pageable) {
        PagedResponse<UserProjectResponseDTO> projects = projectService.getAllProjectsByUser(pageable, filter, authenticatedUser.getUser());
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/accept-invite") // para o browser GET permite clique no link
    public ResponseEntity<MessageResponseDTO> acceptInvitation(@RequestParam String token) {
        projectService.acceptInvitation(token);
        return ResponseEntity.ok(new MessageResponseDTO("Invitation accepted successfully."));
    }

    @GetMapping("/{projectId}/members")
    public ResponseEntity<PagedResponse<ProjectMemberResponseDTO>> getAllMembersFromProject(
            @PathVariable Long projectId,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser,
            Pageable pageable) {
        PagedResponse<ProjectMemberResponseDTO> usersFromProject = projectService.getAllMembersFromProject(pageable, projectId, authenticatedUser.getUser());
        return ResponseEntity.ok(usersFromProject);
    }

    @GetMapping(value = "/{projectId}/export/pdf")
    public ResponseEntity<byte[]> exportProjectToPdf(
            @PathVariable Long projectId,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {

        Project project = projectService.getProjectWithMembersAndTasks(projectId, authenticatedUser.getUser());
        byte[] pdfReportBytes = pdfExportProjectService.export(project);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);

        String fileName = ReportFileNameGenerator.gerenateForProject(project);
        headers.setContentDispositionFormData("attachment", fileName);

        return ResponseEntity.ok().headers(headers).body(pdfReportBytes);
    }

    @PatchMapping("/{projectId}")
    public ResponseEntity<ProjectResponseDTO> updateProject(
            @PathVariable Long projectId,
            @RequestBody @Valid ProjectRequestUpdateDTO requestDTO,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        ProjectResponseDTO updatedProject = projectService.updateProject(projectId, requestDTO, authenticatedUser.getUser());
        return ResponseEntity.ok(updatedProject);
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(
            @PathVariable Long projectId,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        projectService.deleteProject(projectId, authenticatedUser.getUser());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{projectId}/member/{userId}")
    public ResponseEntity<Void> deleteMembershipFromProject(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        projectService.deleteMembershipFromProject(projectId, userId, authenticatedUser.getUser());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{projectId}/dashboard/summary")
    public ResponseEntity<DashboardSummaryDTO> getProjectDashboardSummary(
            @PathVariable Long projectId,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser
    ) {
        DashboardSummaryDTO summary = projectService.getDashboard(projectId, authenticatedUser.getUser());
        return ResponseEntity.ok(summary);
    }


}

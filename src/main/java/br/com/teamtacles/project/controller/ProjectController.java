package br.com.teamtacles.project.controller;

import br.com.teamtacles.common.dto.page.PagedResponse;
import br.com.teamtacles.project.dto.request.ProjectRequestRegisterDTO;
import br.com.teamtacles.project.dto.response.ProjectResponseDTO;
import br.com.teamtacles.project.service.ProjectService;
import br.com.teamtacles.security.UserAuthenticated;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;

@RestController
@RequestMapping("/api/project")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public ResponseEntity<ProjectResponseDTO> createProject(
            @RequestBody @Valid ProjectRequestRegisterDTO requestDTO,
            @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        ProjectResponseDTO responseDTO = projectService.createProject(requestDTO, authenticatedUser.getUser());
        return ResponseEntity.ok(responseDTO);
    }
    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectResponseDTO> getProjectById(@PathVariable Long projectId, @AuthenticationPrincipal UserAuthenticated authenticatedUser) {
        ProjectResponseDTO projectDTO = projectService.getProjectById(projectId, authenticatedUser.getUser());
        return ResponseEntity.ok(projectDTO);
    }

    @GetMapping
    public ResponseEntity<PagedResponse<ProjectResponseDTO>> getAllProjectsByUser(
            @AuthenticationPrincipal UserAuthenticated authenticatedUser,
            Pageable pageable) {
        PagedResponse<ProjectResponseDTO> projects = projectService.getAllProjectsByUser(pageable, authenticatedUser.getUser());
        return ResponseEntity.ok(projects);
    }
}

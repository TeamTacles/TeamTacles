package br.com.teamtacles.infrastructure.export;

import br.com.teamtacles.common.exception.PdfGenerationException;
import br.com.teamtacles.common.util.ReportFileNameGenerator;
import br.com.teamtacles.project.dto.response.PdfExportResult;
import br.com.teamtacles.project.model.ProjectMember;
import br.com.teamtacles.task.dto.response.TaskSummaryDTO;
import br.com.teamtacles.project.model.Project;
import br.com.teamtacles.project.service.ProjectService;
import br.com.teamtacles.task.dto.request.TaskFilterReportDTO;
import br.com.teamtacles.task.model.Task;
import br.com.teamtacles.task.service.TaskService;
import br.com.teamtacles.user.model.User;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProjectPdfExportService {

    private final TemplateEngine templateEngine;
    private final ProjectService projectService;

    public ProjectPdfExportService(TemplateEngine templateEngine, ProjectService projectService, TaskService taskService) {
        this.templateEngine = templateEngine;
        this.projectService = projectService;
    }

    public PdfExportResult generateProjectPdf(Long projectId, User actingUser, TaskFilterReportDTO taskFilter) {
        Project project = projectService.getProjectByIdForReport(projectId, taskFilter.getAssignedUserId(), actingUser);
        Set<Task> tasks = projectService.findFilteredTasksForProject(projectId, taskFilter);

        List<Task> tasksSorted = tasks.stream()
                .sorted(Comparator.comparingInt((Task task) -> task.getEffectiveStatus().getValue()).reversed())
                .toList();

        List<ProjectMember> membersSorted = project.getMembers().stream()
                .sorted(Comparator.comparingInt(member -> member.getProjectRole().getValue()))
                .toList();

        TaskSummaryDTO summary = projectService.calculateTaskSummary(tasks);

        byte[] pdfContent = export(project, summary, membersSorted, tasksSorted, taskFilter);

        String filename = ReportFileNameGenerator.gerenateForProject(project);

        return new PdfExportResult(pdfContent, filename);
    }

    public byte[] export(Project project, TaskSummaryDTO summary, List<ProjectMember> membersSorted, List<Task> tasksSorted, TaskFilterReportDTO taskFilter) {
        try {

            String logoDataUri = loadLogoAsBase64("static/images/icon.png");

            Context context = new Context();
            context.setVariable("project", project);
            context.setVariable("summary", summary);
            context.setVariable("membersSorted", membersSorted);
            context.setVariable("tasks", tasksSorted);
            context.setVariable("taskFilter", taskFilter);
            context.setVariable("logoUrl", logoDataUri);
            context.setVariable("generationDate", OffsetDateTime.now());

            String html = templateEngine.process("project-report-template", context);

            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                PdfRendererBuilder builder = new PdfRendererBuilder();
                builder.useFastMode();
                builder.withHtmlContent(html, null);
                builder.toStream(out);
                builder.run();
                return out.toByteArray();
            }

        } catch (IOException e) {
            throw new PdfGenerationException("Failed to generate PDF report.");
        }
    }

    private String loadLogoAsBase64(String classpathResource) throws IOException {
        ClassPathResource logoResource = new ClassPathResource(classpathResource);
        if (!logoResource.exists()) {
            return null;
        }

        try(InputStream inputStream = logoResource.getInputStream()) {
            byte[] logoBytes = inputStream.readAllBytes();
            String logoBase64 = Base64.getEncoder().encodeToString(logoBytes);
            return "data:image/png;base64," + logoBase64;
        }
    }
}
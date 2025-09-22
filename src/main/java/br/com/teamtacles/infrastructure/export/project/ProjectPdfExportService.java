package br.com.teamtacles.infrastructure.export.project;

import br.com.teamtacles.common.exception.PdfGenerationException;
import br.com.teamtacles.common.util.ReportFileNameGenerator;
import br.com.teamtacles.infrastructure.export.project.dto.PdfExportResult;
import br.com.teamtacles.project.dto.common.TaskSummaryDTO;
import br.com.teamtacles.project.model.Project;
import br.com.teamtacles.project.service.ProjectService;
import br.com.teamtacles.task.dto.request.TaskFilterDTO;
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
import java.util.Base64;

@Service
public class ProjectPdfExportService {

    private final TemplateEngine templateEngine;
    private final ProjectService projectService;
    private final TaskService taskService;

    public ProjectPdfExportService(TemplateEngine templateEngine, ProjectService projectService, TaskService taskService) {
        this.templateEngine = templateEngine;
        this.projectService = projectService;
        this.taskService = taskService;
    }

    public PdfExportResult generateProjectPdf(Long projectId, User actingUser, TaskFilterDTO taskFilter) {
        Project project = projectService.getProjectByIdForReport(projectId, taskFilter.getAssignedUserId(), actingUser);
        project.setTasks(taskService.findFilteredTasksForProject(projectId, taskFilter));
        TaskSummaryDTO summary = taskService.calculateTaskSummary(project.getTasks());
        byte[] pdfContent = export(project, summary);

        String filename = ReportFileNameGenerator.gerenateForProject(project);

        return new PdfExportResult(pdfContent, filename);
    }

    public byte[] export(Project project, TaskSummaryDTO summary) {
        try {

            String logoDataUri = loadLogoAsBase64("static/images/logo.png");

            Context context = new Context();
            context.setVariable("project", project);
            context.setVariable("summary", summary);
            context.setVariable("logoUrl", logoDataUri);

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
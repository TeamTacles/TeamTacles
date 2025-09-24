package br.com.teamtacles.infrastructure.export;

import br.com.teamtacles.common.exception.PdfGenerationException;
import br.com.teamtacles.common.util.ReportFileNameGenerator;
import br.com.teamtacles.project.dto.request.PdfExportResult;
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
import java.util.Base64;
import java.util.Set;

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
        TaskSummaryDTO summary = projectService.calculateTaskSummary(tasks);

        byte[] pdfContent = export(project, summary, tasks, taskFilter);

        String filename = ReportFileNameGenerator.gerenateForProject(project);

        return new PdfExportResult(pdfContent, filename);
    }

    public byte[] export(Project project, TaskSummaryDTO summary, Set<Task> tasks, TaskFilterReportDTO taskFilter) {
        try {

            String logoDataUri = loadLogoAsBase64("static/images/logo.png");

            Context context = new Context();
            context.setVariable("project", project);
            context.setVariable("summary", summary);
            context.setVariable("tasks", tasks);
            context.setVariable("taskFilter", taskFilter);
            context.setVariable("logoUrl", logoDataUri);
            context.setVariable("generationDate", LocalDateTime.now());

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
package br.com.teamtacles.infrastructure.export;

import br.com.teamtacles.common.exception.PdfGenerationException;
import br.com.teamtacles.project.dto.common.TaskSummaryDTO;
import br.com.teamtacles.project.model.Project;
import br.com.teamtacles.project.service.ProjectService;
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

    public ProjectPdfExportService(TemplateEngine templateEngine, ProjectService projectService) {
        this.templateEngine = templateEngine;
        this.projectService = projectService;
    }

    public byte[] export(Project project) {
        try {
            TaskSummaryDTO summary = projectService.calculateTaskSummary(project.getTasks());
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
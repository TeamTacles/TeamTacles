package br.com.teamtacles.project.service;

import br.com.teamtacles.common.exception.PdfGenerationException;
import br.com.teamtacles.project.dto.report.TaskSummary;
import br.com.teamtacles.project.model.Project;
import br.com.teamtacles.task.enumeration.ETaskStatus;
import br.com.teamtacles.task.model.Task;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PdfExportProjectService {

    private final TemplateEngine templateEngine;

    public byte[] export(Project project) {
        try {
            TaskSummary summary = calculateTaskSummary(project.getTasks());
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

    private TaskSummary calculateTaskSummary(Set<Task> tasks) {
        long doneCount = tasks.stream().filter(t -> t.getStatus() == ETaskStatus.DONE).count();
        long inProgressCount = tasks.stream().filter(t -> t.getStatus() == ETaskStatus.IN_PROGRESS).count();
        long toDoCount = tasks.stream().filter(t -> t.getStatus() == ETaskStatus.TO_DO).count();
        long overdueCount = tasks.stream().filter(t -> t.getDueDate() != null && t.getDueDate().isBefore(OffsetDateTime.now()) && t.getStatus() != ETaskStatus.DONE).count();
        return new TaskSummary(doneCount, inProgressCount, toDoCount, overdueCount);
    }

    private String loadLogoAsBase64(String classpathResource) throws IOException {
        ClassPathResource logoResource = new ClassPathResource(classpathResource);
        if (!logoResource.exists()) {
            return null;
        }
        byte[] logoBytes = Files.readAllBytes(logoResource.getFile().toPath());
        String logoBase64 = Base64.getEncoder().encodeToString(logoBytes);
        return "data:image/png;base64," + logoBase64;
    }
}
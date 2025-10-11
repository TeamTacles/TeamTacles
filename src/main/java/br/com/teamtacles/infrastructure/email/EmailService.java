package br.com.teamtacles.infrastructure.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);


    @Value("${app.base-url}")
    private String baseUrl;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    @Async
    public void sendPasswordResetEmail(String to, String resetUrl) {
        // DIAGNÓSTICO DEFINITIVO: O que está nesta linha?
        log.info("[DIAGNOSTIC-TEST] Valor de 'resetUrl' recebido pelo EmailService: {}", resetUrl);

        log.info("[EMAIL-ACTION] Attempting to send 'password-reset-email' to '{}'", to);

        try {
            Context context = new Context();
            context.setVariable("resetUrl", resetUrl);

            String htmlContent = templateEngine.process("password-reset-email", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("🐙 TeamTacles - Password Reset Request");
            helper.setText(htmlContent, true);
            helper.addInline("logo", new ClassPathResource("static/images/Reset_Password.png"));

            mailSender.send(message);
            log.info("[EMAIL-SUCCESS] Successfully sent 'password-reset-email' to '{}'", to);

        } catch (MessagingException e) {
            log.error("[EMAIL-FAILURE] Failed to send 'password-reset-email' to '{}'. Error: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    @Async
    public void sendTeamInvitationEmail(String to, String teamName, String token) {
        log.info("[EMAIL-ACTION] Attempting to send 'team-invitation-email' to '{}' for team '{}'", to, teamName);

        try {
            Context context = new Context();
            context.setVariable("teamName", teamName);
            context.setVariable("invitationUrl", baseUrl + "/api/team/accept-invite-email?token=" + token);

            String htmlContent = templateEngine.process("team-invitation-email", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("🐙 TeamTacles - Team Invitation");
            helper.setText(htmlContent, true);
            helper.addInline("teamLogo", new ClassPathResource("static/images/Invite_team.png"));

            mailSender.send(message);
            log.info("[EMAIL-SUCCESS] Successfully sent 'team-invitation-email' to '{}' for team '{}'", to, teamName);
        } catch (MessagingException e) {
            log.error("[EMAIL-FAILURE] Failed to send 'team-invitation-email' to '{}' for team '{}'. Error: {}", to, teamName, e.getMessage(), e);

            throw new RuntimeException("Failed to send invitation email", e);
        }
    }


    @Async
    public void sendProjectInvitationEmail(String to, String projectName, String token) {
        log.info("[EMAIL-ACTION] Attempting to send 'project-invitation-email' to '{}' for project '{}'", to, projectName);
        try {
            Context context = new Context();
            context.setVariable("projectName", projectName);
            context.setVariable("invitationUrl", baseUrl + "/api/project/accept-invite-email?token=" + token);

            String htmlContent = templateEngine.process("project-invitation-email", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("🐙 TeamTacles - Project Invitation");
            helper.setText(htmlContent, true);

            helper.addInline("projectLogo", new ClassPathResource("static/images/Invite_Project.png"));

            mailSender.send(message);
            log.info("[EMAIL-SUCCESS] Successfully sent 'project-invitation-email' to '{}' for project '{}'", to, projectName);
        } catch (MessagingException e) {
            log.error("[EMAIL-FAILURE] Failed to send 'project-invitation-email' to '{}' for project '{}'. Error: {}", to, projectName, e.getMessage(), e);
            throw new RuntimeException("Failed to send project invitation email", e);
        }
    }

    @Async
    public void sendVerificationEmail(String to, String token) {
        log.info("[EMAIL-ACTION] Attempting to send 'verification-email' to '{}'", to);
        try {
            Context context = new Context();
            context.setVariable("verificationUrl", baseUrl + "/api/user/verify-account?token=" + token);

            String htmlContent = templateEngine.process("verification-email", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("🐙 TeamTacles - Confirm your account");
            helper.setText(htmlContent, true);
            helper.addInline("welcomeLogo", new ClassPathResource("static/images/Welcome_message.png"));

            mailSender.send(message);
            log.info("[EMAIL-SUCCESS] Successfully sent 'verification-email' to '{}'", to);
        } catch (MessagingException e) {
            log.error("[EMAIL-FAILURE] Failed to send 'verification-email' to '{}'. Error: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

}
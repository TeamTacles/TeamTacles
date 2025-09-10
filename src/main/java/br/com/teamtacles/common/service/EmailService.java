package br.com.teamtacles.common.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailService {

    @Value("${app.base-url}")
    private String baseUrl;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    @Async
    public void sendPasswordResetEmail(String to, String token) {
        try {
            Context context = new Context();
            context.setVariable("resetUrl", baseUrl + "/reset-password?token=" + token);

            String htmlContent = templateEngine.process("password-reset-email", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("🐙 TeamTacles - Password Reset Request");
            helper.setText(htmlContent, true);
            helper.addInline("logo", new ClassPathResource("static/images/Reset_Password.png"));

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email", e);
        }
    }

    @Async
    public void sendTeamInvitationEmail(String to, String teamName, String token) {
        try {
            Context context = new Context();
            context.setVariable("teamName", teamName);
            context.setVariable("invitationUrl", baseUrl + "/accept-invite?token=" + token);

            String htmlContent = templateEngine.process("team-invitation-email", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("🐙 TeamTacles - Team Invitation");
            helper.setText(htmlContent, true);
            helper.addInline("teamLogo", new ClassPathResource("static/images/Invite_team.png"));

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send invitation email", e);
        }
    }

    @Async
    public void sendVerificationEmail(String to, String token) {
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
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

}
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

@Service
public class EmailService {

    @Value("${app.base-url}")
    private String baseUrl;

    @Autowired
    private JavaMailSender mailSender;

    @Async
    public void sendPasswordResetEmail(String to, String token) {

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(to);
            helper.setSubject("🐙 TeamTacles - Password Reset Request");


            String resetUrl = baseUrl + "/reset-password?token=" + token;

            String htmlContent = "<html><body>" +
                    "Hello,<br><br>You requested a password reset. " +
                    "Click the link below to create a new password:<br><br>" +
                    "<a href='" + resetUrl + "'>" + resetUrl + "</a>" +
                    "<br><br>If you did not request this change, please ignore this email." +
                    "<br><br>Best regards,<br>TeamTacles 🐙" +
                    "<br><br><hr><br>" +
                    "<img src='cid:logo' style='width:500px; height:auto; display:block; margin:0 auto;'>" +
                    "</body></html>";

            helper.setText(htmlContent, true);

            ClassPathResource logo = new ClassPathResource("static/images/Reset_Password.png");
            helper.addInline("logo", logo);
            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email", e);
        }

    }

    @Async
    public void sendTeamInvitationEmail(String to, String teamName, String token) {

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(to);
            helper.setSubject("🐙 TeamTacles - Team Invitation");

            String invitationUrl = baseUrl + "/accept-invite?token=" + token;

            String htmlContent = "<html><body>" +
                    "Hello,<br><br>You have been invited to join the team <strong>" + teamName + "</strong>. " +
                    "Click the link below to accept the invitation:<br><br>" +
                    "<a href='" + invitationUrl + "'>" + invitationUrl + "</a>" +
                    "<br><br>If you were not expecting this invitation, please ignore this email." +
                    "<br><br>Sincerely,<br>The TeamTacles Team 🐙" +
                    "<br><br><hr><br>" +
                    "<img src='cid:teamLogo' style='width:500px; height:auto; display:block; margin:0 auto;'>" +
                    "</body></html>";

            helper.setText(htmlContent, true);
            ClassPathResource logo = new ClassPathResource("static/images/Invite_team.png");
            helper.addInline("teamLogo", logo);
            mailSender.send(message);


        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send invitation email", e);
        }


    }
}
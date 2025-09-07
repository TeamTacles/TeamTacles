package br.com.teamtacles.common.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Async
    public void sendPasswordResetEmail(String to, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("TeamTacles -  Password Reset Request");

      String resetUrl = "http://localhost:8080/reset-password?token=" + token;

                message.setText("Hello,\n\nYou requested a password reset. " +
                        "Click the link below to create a new password:\n\n" + resetUrl +
                        "\n\nIf you did not request this change, please ignore this email." +
                        "\n\nBest regards,\n TeamTacles");

        mailSender.send(message);
    }

    @Async
    public void sendTeamInvitationEmail(String to, String teamName, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("You have been invited to a TeamTacles team " + teamName);

        String invitationUrl =   "http://localhost:8080/accept-invite?token=" + token;

        message.setText("Hello,\n\nYou have been invited to join the team " + teamName + ". " +
                "Click the link below to accept the invitation:\n\n" + invitationUrl +
                "\n\nIf you were not expecting this invitation, please ignore this email." +
                "\n\nSincerely,\nThe TeamTacles Team");

        mailSender.send(message);


    }
}
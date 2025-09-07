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

      String resetUrl = "http://localhost:3000/reset-password?token=" + token;

                message.setText("Hello,\n\nYou requested a password reset. " +
                        "Click the link below to create a new password:\n\n" + resetUrl +
                        "\n\nIf you did not request this change, please ignore this email." +
                        "\n\nBest regards,\n TeamTacles");

        mailSender.send(message);
    }
}
package com.nirmala.logsense.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailAlertService {

    private final JavaMailSender mailSender;

    public EmailAlertService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendAlert(String toEmail, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}
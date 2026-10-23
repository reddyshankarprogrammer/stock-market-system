package com.stockmarket.userservice.service.impl;

import com.stockmarket.userservice.service.MailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * SMTP sender. The JavaMailSender only exists when spring.mail.host is set (it is in the main configuration; the
 * tests do not set it), so it is looked up lazily. Failures are logged, never thrown, and never log the message
 * body, which contains a reset link.
 */
@Service
@Slf4j
public class SmtpMailService implements MailService {

    private final ObjectProvider<JavaMailSender> sender;
    private final String from;

    public SmtpMailService(ObjectProvider<JavaMailSender> sender, @Value("${app.mail-from:no-reply@quantiva.local}") String from) {
        this.sender = sender;
        this.from = from;
    }

    @Override
    public void send(String to, String subject, String body) {
        JavaMailSender mail = sender.getIfAvailable();
        if (mail == null) {
            log.warn("Mail is not configured (spring.mail.host); not sending \"{}\"", subject);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mail.send(message);
            log.info("Email sent: \"{}\"", subject);
        } catch (Exception e) {
            log.error("Could not send email \"{}\": {}", subject, e.getMessage());
        }
    }
}

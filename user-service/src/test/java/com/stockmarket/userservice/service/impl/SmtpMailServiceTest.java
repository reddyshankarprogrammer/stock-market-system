package com.stockmarket.userservice.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmtpMailServiceTest {

    @Mock
    private ObjectProvider<JavaMailSender> provider;
    @Mock
    private JavaMailSender sender;

    @Test
    void sendsAPlainTextMessageFromTheConfiguredAddress() {
        when(provider.getIfAvailable()).thenReturn(sender);

        new SmtpMailService(provider, "no-reply@quantiva.local").send("asha@example.com", "Subject", "Body");

        ArgumentCaptor<SimpleMailMessage> sent = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(sent.capture());
        assertEquals("no-reply@quantiva.local", sent.getValue().getFrom());
        assertArrayEquals(new String[] {"asha@example.com"}, sent.getValue().getTo());
        assertEquals("Subject", sent.getValue().getSubject());
        assertEquals("Body", sent.getValue().getText());
    }

    @Test
    void aFailingMailServer_neverThrows() {
        when(provider.getIfAvailable()).thenReturn(sender);
        doThrow(new MailSendException("connection refused")).when(sender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> new SmtpMailService(provider, "x@y.z").send("a@b.c", "S", "B"));
    }

    @Test
    void whenMailIsNotConfigured_itSkipsQuietlyAndNeverThrows() {
        when(provider.getIfAvailable()).thenReturn(null);

        assertDoesNotThrow(() -> new SmtpMailService(provider, "x@y.z").send("a@b.c", "S", "B"));
        verifyNoInteractions(sender);
    }
}

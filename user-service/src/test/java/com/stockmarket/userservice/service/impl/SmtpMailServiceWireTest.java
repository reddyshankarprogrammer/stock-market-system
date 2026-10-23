package com.stockmarket.userservice.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Sends through a real JavaMailSender to a tiny in-process SMTP server, so the message really goes over the wire
 * (Mailpit in docker compose is not needed for the build). Proves from, to, subject and body arrive.
 */
class SmtpMailServiceWireTest {

    @Test
    @SuppressWarnings("unchecked")
    void theEmailReallyArrivesOverSmtp() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            CompletableFuture<String> received = CompletableFuture.supplyAsync(() -> {
                StringBuilder all = new StringBuilder();
                try (Socket socket = server.accept();
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8)) {
                    out.print("220 fake ready\r\n");
                    out.flush();
                    boolean data = false;
                    String line;
                    while ((line = in.readLine()) != null) {
                        all.append(line).append('\n');
                        if (data) {
                            if (line.equals(".")) {
                                data = false;
                                out.print("250 queued\r\n");
                                out.flush();
                            }
                            continue;
                        }
                        String cmd = line.toUpperCase();
                        if (cmd.startsWith("EHLO") || cmd.startsWith("HELO")) out.print("250 hello\r\n");
                        else if (cmd.startsWith("DATA")) { out.print("354 go ahead\r\n"); data = true; }
                        else if (cmd.startsWith("QUIT")) { out.print("221 bye\r\n"); out.flush(); break; }
                        else out.print("250 ok\r\n");
                        out.flush();
                    }
                } catch (Exception ignored) {
                    // the assertions below report what was (not) received
                }
                return all.toString();
            });

            JavaMailSenderImpl real = new JavaMailSenderImpl();
            real.setHost("localhost");
            real.setPort(server.getLocalPort());
            ObjectProvider<org.springframework.mail.javamail.JavaMailSender> provider = mock(ObjectProvider.class);
            when(provider.getIfAvailable()).thenReturn(real);

            new SmtpMailService(provider, "no-reply@quantiva.local")
                    .send("asha@example.com", "Reset your Quantiva password", "Open http://localhost:3000/reset-password?token=abc");

            String wire = received.get(10, java.util.concurrent.TimeUnit.SECONDS);
            assertTrue(wire.contains("MAIL FROM:<no-reply@quantiva.local>"), wire);
            assertTrue(wire.contains("RCPT TO:<asha@example.com>"), wire);
            assertTrue(wire.contains("Subject: Reset your Quantiva password"), wire);
            assertTrue(wire.contains("reset-password?token=abc"), wire);
        }
    }
}

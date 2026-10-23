package com.stockmarket.userservice.service;

/** Sends plain-text email. Implementations must not throw: a failed email never changes an API answer. */
public interface MailService {

    void send(String to, String subject, String body);
}

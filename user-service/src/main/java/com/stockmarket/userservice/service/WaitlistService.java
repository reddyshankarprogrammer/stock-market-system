package com.stockmarket.userservice.service;

import com.stockmarket.userservice.dto.WaitlistRequest;
import com.stockmarket.userservice.entity.WaitlistEntry;
import com.stockmarket.userservice.repository.WaitlistEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * Public sign-up for the launch list. Joining twice is not an error and looks the same as joining once, so the answer
 * cannot be used to find out who is on the list. The email address is personal data: it is never written to the log.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WaitlistService {

    private final WaitlistEntryRepository repository;

    public void join(WaitlistRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (repository.existsByEmail(email)) {
            return;
        }
        WaitlistEntry entry = new WaitlistEntry();
        entry.setEmail(email);
        entry.setConsentedAt(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS));
        entry.setSource(request.getSource());
        try {
            repository.save(entry);
            log.info("Waitlist entry added");
        } catch (DataIntegrityViolationException e) {
            // Two identical requests at once: the unique key let one in, which is exactly the outcome wanted.
            log.debug("Waitlist entry already present");
        }
    }
}

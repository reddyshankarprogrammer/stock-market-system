package com.stockmarket.userservice.service;

import com.stockmarket.userservice.dto.WaitlistRequest;
import com.stockmarket.userservice.entity.WaitlistEntry;
import com.stockmarket.userservice.repository.WaitlistEntryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WaitlistServiceTest {

    @Mock
    private WaitlistEntryRepository repository;
    @InjectMocks
    private WaitlistService service;

    @Test
    void aNewEmailIsStoredInLowerCase_withTheTimeOfConsent() {
        service.join(new WaitlistRequest("  Asha.Rao@Example.COM ", true, "landing"));

        ArgumentCaptor<WaitlistEntry> saved = ArgumentCaptor.forClass(WaitlistEntry.class);
        verify(repository).save(saved.capture());
        assertEquals("asha.rao@example.com", saved.getValue().getEmail());
        assertEquals("landing", saved.getValue().getSource());
        assertNotNull(saved.getValue().getConsentedAt());
        assertEquals(0, saved.getValue().getConsentedAt().getNano() % 1000, "microsecond precision, like MySQL");
    }

    @Test
    void anEmailThatIsAlreadyListedIsNotAnError_andIsNotStoredTwice() {
        when(repository.existsByEmail("asha@example.com")).thenReturn(true);

        assertDoesNotThrow(() -> service.join(new WaitlistRequest("Asha@example.com", true, null)));

        verify(repository, never()).save(any());
    }

    @Test
    void twoIdenticalRequestsAtOnce_endWithOneEntry_andNoError() {
        when(repository.save(any())).thenThrow(new DataIntegrityViolationException("uk_waitlist_email"));

        assertDoesNotThrow(() -> service.join(new WaitlistRequest("asha@example.com", true, null)));
    }
}

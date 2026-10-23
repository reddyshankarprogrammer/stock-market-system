package com.stockmarket.userservice.service;

import com.stockmarket.userservice.dto.OnboardingRequest;
import com.stockmarket.userservice.dto.OnboardingResponse;
import com.stockmarket.userservice.entity.User;
import com.stockmarket.userservice.exception.UserNotFoundException;
import com.stockmarket.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceTest {

    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private OnboardingService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId("user-1");
        lenient().when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
    }

    private OnboardingRequest request(String experience, List<String> brokers, String plan) {
        return new OnboardingRequest(experience, brokers, plan);
    }

    @Test
    void newUser_hasNothingAnsweredAndIsNotCompleted() {
        OnboardingResponse r = service.get("user-1");

        assertNull(r.getTradingExperience());
        assertTrue(r.getBrokerPreferences().isEmpty());
        assertNull(r.getPlan());
        assertFalse(r.isCompleted());
        assertNull(r.getCompletedAt());
    }

    @Test
    void savingOneStep_changesOnlyThatField_soTheWizardCanBeResumed() {
        service.save("user-1", request("ADVANCED", null, null));
        assertEquals("ADVANCED", user.getTradingExperience());
        assertNull(user.getBrokerPreferences());
        assertNull(user.getSelectedPlan());

        service.save("user-1", request(null, List.of("ZERODHA", "GROWW"), null));
        assertEquals("ADVANCED", user.getTradingExperience(), "an earlier answer is kept");
        assertEquals("GROWW,ZERODHA", user.getBrokerPreferences(), "stored sorted");

        OnboardingResponse r = service.save("user-1", request(null, null, "PRO"));
        assertEquals("PRO", r.getPlan());
        assertEquals(List.of("GROWW", "ZERODHA"), r.getBrokerPreferences());
        verify(userRepository, times(3)).save(user);
    }

    @Test
    void brokers_areDeduplicated_andNoneCannotBeCombined() {
        service.save("user-1", request(null, List.of("GROWW", "GROWW", "BINANCE"), null));
        assertEquals("BINANCE,GROWW", user.getBrokerPreferences());

        service.save("user-1", request(null, List.of("NONE"), null));
        assertEquals("NONE", user.getBrokerPreferences());

        assertThrows(IllegalArgumentException.class, () -> service.save("user-1", request(null, List.of("NONE", "GROWW"), null)));
        assertEquals("NONE", user.getBrokerPreferences(), "a rejected save changes nothing");
        assertThrows(IllegalArgumentException.class, () -> service.save("user-1", request(null, List.of(), null)));
    }

    @Test
    void complete_needsAllThreeAnswers() {
        assertThrows(IllegalArgumentException.class, () -> service.complete("user-1"));

        service.save("user-1", request("BEGINNER", null, null));
        assertThrows(IllegalArgumentException.class, () -> service.complete("user-1"));
        service.save("user-1", request(null, List.of("NONE"), null));
        assertThrows(IllegalArgumentException.class, () -> service.complete("user-1"));
        assertNull(user.getOnboardingCompletedAt(), "nothing was marked complete along the way");

        service.save("user-1", request(null, null, "FREE"));
        OnboardingResponse r = service.complete("user-1");
        assertTrue(r.isCompleted());
        assertNotNull(user.getOnboardingCompletedAt());
        assertEquals(0, user.getOnboardingCompletedAt().getNano() % 1000, "stored at microsecond precision, like MySQL, so every response shows the same value");
    }

    @Test
    void completingTwice_keepsTheFirstCompletionTime() {
        LocalDateTime first = LocalDateTime.of(2026, 10, 24, 12, 0);
        user.setTradingExperience("BEGINNER");
        user.setBrokerPreferences("NONE");
        user.setSelectedPlan("FREE");
        user.setOnboardingCompletedAt(first);

        OnboardingResponse r = service.complete("user-1");

        assertEquals(first, r.getCompletedAt());
        verify(userRepository, never()).save(any());
    }

    @Test
    void aUserThatNoLongerExists_isNotFound() {
        when(userRepository.findById("ghost")).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> service.get("ghost"));
        assertThrows(UserNotFoundException.class, () -> service.save("ghost", request("BEGINNER", null, null)));
        assertThrows(UserNotFoundException.class, () -> service.complete("ghost"));
    }
}

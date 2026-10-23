package com.stockmarket.userservice.service;

import com.stockmarket.userservice.dto.OnboardingRequest;
import com.stockmarket.userservice.dto.OnboardingResponse;
import com.stockmarket.userservice.entity.User;
import com.stockmarket.userservice.exception.UserNotFoundException;
import com.stockmarket.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * The answers collected by the onboarding wizard. Saved step by step (only the fields sent change), so a
 * half-finished onboarding can be resumed; {@link #complete} needs all three answers. Always works on the caller's own
 * record: the user id comes from the gateway's X-User-Id header, never from a request body.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OnboardingService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public OnboardingResponse get(String userId) {
        return toResponse(load(userId));
    }

    @Transactional
    public OnboardingResponse save(String userId, OnboardingRequest request) {
        User user = load(userId);

        if (request.getTradingExperience() != null) {
            user.setTradingExperience(request.getTradingExperience());
        }
        if (request.getBrokerPreferences() != null) {
            Set<String> brokers = new TreeSet<>(request.getBrokerPreferences());
            if (brokers.isEmpty()) {
                throw new IllegalArgumentException("Choose at least one broker, or NONE");
            }
            if (brokers.contains("NONE") && brokers.size() > 1) {
                throw new IllegalArgumentException("NONE cannot be combined with other brokers");
            }
            user.setBrokerPreferences(String.join(",", brokers));
        }
        if (request.getPlan() != null) {
            user.setSelectedPlan(request.getPlan());
        }
        userRepository.save(user);
        log.info("Onboarding saved for user: {}", userId);
        return toResponse(user);
    }

    @Transactional
    public OnboardingResponse complete(String userId) {
        User user = load(userId);
        if (user.getTradingExperience() == null || user.getBrokerPreferences() == null || user.getSelectedPlan() == null) {
            throw new IllegalArgumentException("Choose your trading experience, your brokers and a plan first");
        }
        if (user.getOnboardingCompletedAt() == null) { // completing twice keeps the first time
            // Microseconds, the precision MySQL stores: the first response then equals every later one.
            user.setOnboardingCompletedAt(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS));
            userRepository.save(user);
            log.info("Onboarding completed for user: {}", userId);
        }
        return toResponse(user);
    }

    private User load(String userId) {
        return userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
    }

    private OnboardingResponse toResponse(User user) {
        List<String> brokers = user.getBrokerPreferences() == null || user.getBrokerPreferences().isBlank()
                ? List.of()
                : List.copyOf(new LinkedHashSet<>(Arrays.asList(user.getBrokerPreferences().split(","))));
        return new OnboardingResponse(user.getTradingExperience(), brokers, user.getSelectedPlan(),
                user.getOnboardingCompletedAt() != null, user.getOnboardingCompletedAt());
    }
}

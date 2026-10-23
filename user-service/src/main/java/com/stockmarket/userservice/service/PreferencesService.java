package com.stockmarket.userservice.service;

import com.stockmarket.userservice.dto.PreferencesRequest;
import com.stockmarket.userservice.dto.PreferencesResponse;
import com.stockmarket.userservice.entity.UserPreferences;
import com.stockmarket.userservice.exception.UserNotFoundException;
import com.stockmarket.userservice.repository.UserPreferencesRepository;
import com.stockmarket.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Settings of the caller. The user id comes from the gateway's X-User-Id header, never from a request body, so a user
 * can only ever read or change their own. Only the fields sent are changed.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PreferencesService {

    private final UserPreferencesRepository preferencesRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PreferencesResponse get(String userId) {
        requireUser(userId);
        return toResponse(preferencesRepository.findById(userId).orElseGet(() -> new UserPreferences(userId)));
    }

    @Transactional
    public PreferencesResponse save(String userId, PreferencesRequest request) {
        requireUser(userId);
        UserPreferences p = preferencesRepository.findById(userId).orElseGet(() -> new UserPreferences(userId));

        if (request.getTheme() != null) p.setTheme(request.getTheme());
        if (request.getLanguage() != null) p.setLanguage(request.getLanguage());
        if (request.getNotifyInApp() != null) p.setNotifyInApp(request.getNotifyInApp());
        if (request.getNotifyPush() != null) p.setNotifyPush(request.getNotifyPush());
        if (request.getNotifyEmail() != null) p.setNotifyEmail(request.getNotifyEmail());
        if (request.getExportFormat() != null) p.setExportFormat(request.getExportFormat());
        p.setUpdatedAt(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS));

        preferencesRepository.save(p);
        log.info("Preferences saved for user: {}", userId);
        return toResponse(p);
    }

    private void requireUser(String userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException();
        }
    }

    private static PreferencesResponse toResponse(UserPreferences p) {
        return new PreferencesResponse(p.getTheme(), p.getLanguage(), p.isNotifyInApp(), p.isNotifyPush(), p.isNotifyEmail(), p.getExportFormat());
    }
}

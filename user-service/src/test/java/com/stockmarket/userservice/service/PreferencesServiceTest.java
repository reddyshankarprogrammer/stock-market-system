package com.stockmarket.userservice.service;

import com.stockmarket.userservice.dto.PreferencesRequest;
import com.stockmarket.userservice.dto.PreferencesResponse;
import com.stockmarket.userservice.entity.UserPreferences;
import com.stockmarket.userservice.exception.UserNotFoundException;
import com.stockmarket.userservice.repository.UserPreferencesRepository;
import com.stockmarket.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PreferencesServiceTest {

    @Mock
    private UserPreferencesRepository preferencesRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private PreferencesService service;

    @BeforeEach
    void setUp() {
        lenient().when(userRepository.existsById("user-1")).thenReturn(true);
    }

    @Test
    void aUserWhoNeverSavedGetsTheDefaults_andNothingIsWritten() {
        when(preferencesRepository.findById("user-1")).thenReturn(Optional.empty());

        PreferencesResponse r = service.get("user-1");

        assertEquals("SYSTEM", r.getTheme());
        assertEquals("en", r.getLanguage());
        assertTrue(r.isNotifyInApp());
        assertFalse(r.isNotifyPush(), "opt-in channels are off until the user switches them on");
        assertFalse(r.isNotifyEmail());
        assertEquals("CSV", r.getExportFormat());
        verify(preferencesRepository, never()).save(any());
    }

    @Test
    void savingOneField_changesOnlyThatField_andKeepsTheEarlierChoices() {
        UserPreferences existing = new UserPreferences("user-1");
        existing.setTheme("DARK");
        existing.setNotifyEmail(true);
        when(preferencesRepository.findById("user-1")).thenReturn(Optional.of(existing));

        PreferencesRequest request = new PreferencesRequest();
        request.setNotifyPush(true);
        PreferencesResponse r = service.save("user-1", request);

        assertEquals("DARK", r.getTheme(), "an earlier choice is kept");
        assertTrue(r.isNotifyEmail());
        assertTrue(r.isNotifyPush());
        ArgumentCaptor<UserPreferences> saved = ArgumentCaptor.forClass(UserPreferences.class);
        verify(preferencesRepository).save(saved.capture());
        assertEquals("user-1", saved.getValue().getUserId());
        assertNotNull(saved.getValue().getUpdatedAt());
    }

    @Test
    void theFirstSaveCreatesTheRowForTheCaller() {
        when(preferencesRepository.findById("user-1")).thenReturn(Optional.empty());

        PreferencesRequest request = new PreferencesRequest();
        request.setTheme("LIGHT");
        request.setExportFormat("JSON");
        PreferencesResponse r = service.save("user-1", request);

        assertEquals("LIGHT", r.getTheme());
        assertEquals("JSON", r.getExportFormat());
        assertTrue(r.isNotifyInApp(), "fields not sent keep their default");
    }

    @Test
    void aUserThatNoLongerExists_isNotFound_andNothingIsSaved() {
        when(userRepository.existsById("ghost")).thenReturn(false);

        assertThrows(UserNotFoundException.class, () -> service.get("ghost"));
        assertThrows(UserNotFoundException.class, () -> service.save("ghost", new PreferencesRequest()));
        verify(preferencesRepository, never()).save(any());
    }
}

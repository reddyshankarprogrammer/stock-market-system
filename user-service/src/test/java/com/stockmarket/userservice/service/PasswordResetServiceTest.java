package com.stockmarket.userservice.service;

import com.stockmarket.userservice.entity.PasswordResetToken;
import com.stockmarket.userservice.entity.User;
import com.stockmarket.userservice.entity.UserStatus;
import com.stockmarket.userservice.exception.InvalidResetTokenException;
import com.stockmarket.userservice.repository.PasswordResetTokenRepository;
import com.stockmarket.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordResetTokenRepository tokenRepository;
    @Mock
    private MailService mailService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(); // real, so the new password is truly verified

    private PasswordResetService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(userRepository, tokenRepository, passwordEncoder, mailService, "http://localhost:3000/", 30, 60);
        user = new User();
        user.setId("user-1");
        user.setEmail("asha@example.com");
        user.setFirstName("Asha");
        user.setPassword(passwordEncoder.encode("OldPassword1"));
        user.setStatus(UserStatus.ACTIVE);
    }

    /** Request a reset and return the raw token that was emailed. */
    private String requestAndCaptureToken() {
        when(userRepository.findByEmail("asha@example.com")).thenReturn(Optional.of(user));
        when(tokenRepository.findTopByUserIdOrderByCreatedAtDesc("user-1")).thenReturn(Optional.empty());
        service.requestReset("asha@example.com");

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(mailService, timeout(2000)).send(eq("asha@example.com"), eq("Reset your Quantiva password"), body.capture());
        Matcher m = Pattern.compile("http://localhost:3000/reset-password\\?token=([A-Za-z0-9_-]+)").matcher(body.getValue());
        assertTrue(m.find(), "the email must contain the reset link (single slash, base URL trailing slash removed)");
        return m.group(1);
    }

    @Test
    void unknownEmail_sendsNothingAndStoresNothing() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        service.requestReset("nobody@example.com");

        verifyNoInteractions(mailService);
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void knownEmail_storesOnlyTheHash_notTheToken_andEmailsTheLink() {
        String token = requestAndCaptureToken();

        ArgumentCaptor<PasswordResetToken> saved = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(saved.capture());
        PasswordResetToken stored = saved.getValue();
        assertEquals(PasswordResetService.hash(token), stored.getTokenHash());
        assertNotEquals(token, stored.getTokenHash(), "the raw token must never be stored");
        assertEquals(64, stored.getTokenHash().length());
        assertTrue(token.length() >= 43, "256 random bits, base64url");
        assertTrue(stored.getExpiresAt().isAfter(LocalDateTime.now().plusMinutes(29)) && stored.getExpiresAt().isBefore(LocalDateTime.now().plusMinutes(31)));
        assertNull(stored.getUsedAt());
        verify(tokenRepository).voidUnusedTokens(eq("user-1"), any()); // older links stop working
    }

    @Test
    void askingAgainWithinTheCooldown_doesNotSendAnotherEmail() {
        PasswordResetToken recent = new PasswordResetToken();
        recent.setCreatedAt(LocalDateTime.now().minusSeconds(10));
        when(userRepository.findByEmail("asha@example.com")).thenReturn(Optional.of(user));
        when(tokenRepository.findTopByUserIdOrderByCreatedAtDesc("user-1")).thenReturn(Optional.of(recent));

        service.requestReset("asha@example.com");

        verifyNoInteractions(mailService);
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void afterTheCooldown_aNewLinkIsSent() {
        PasswordResetToken old = new PasswordResetToken();
        old.setCreatedAt(LocalDateTime.now().minusMinutes(5));
        when(userRepository.findByEmail("asha@example.com")).thenReturn(Optional.of(user));
        when(tokenRepository.findTopByUserIdOrderByCreatedAtDesc("user-1")).thenReturn(Optional.of(old));

        service.requestReset("asha@example.com");

        verify(mailService, timeout(2000)).send(eq("asha@example.com"), anyString(), anyString());
    }

    @Test
    void blockedAccount_getsNoLink() {
        user.setStatus(UserStatus.SUSPENDED);
        when(userRepository.findByEmail("asha@example.com")).thenReturn(Optional.of(user));

        service.requestReset("asha@example.com");

        verifyNoInteractions(mailService);
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void reset_withTheEmailedToken_changesThePassword_recordsWhen_andVoidsTheOthers() {
        String token = requestAndCaptureToken();
        ArgumentCaptor<PasswordResetToken> saved = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(saved.capture());
        when(tokenRepository.findByTokenHash(PasswordResetService.hash(token))).thenReturn(Optional.of(saved.getValue()));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        LocalDateTime before = LocalDateTime.now();
        service.resetPassword(token, "BrandNewPass9");

        assertTrue(passwordEncoder.matches("BrandNewPass9", user.getPassword()), "new password works");
        assertFalse(passwordEncoder.matches("OldPassword1", user.getPassword()), "old password no longer works");
        assertFalse(user.getPasswordChangedAt().isBefore(before));
        verify(userRepository).save(user);
        verify(tokenRepository, atLeastOnce()).voidUnusedTokens(eq("user-1"), any());
    }

    @Test
    void aTokenWorksOnlyOnce() {
        String token = requestAndCaptureToken();
        ArgumentCaptor<PasswordResetToken> saved = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(saved.capture());
        PasswordResetToken stored = saved.getValue();
        when(tokenRepository.findByTokenHash(PasswordResetService.hash(token))).thenReturn(Optional.of(stored));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        // behave like the real query: voiding marks the token used
        when(tokenRepository.voidUnusedTokens(eq("user-1"), any())).thenAnswer(i -> {
            if (stored.getUsedAt() == null) stored.setUsedAt(i.getArgument(1));
            return 1;
        });

        service.resetPassword(token, "FirstNewPass1");
        assertThrows(InvalidResetTokenException.class, () -> service.resetPassword(token, "SecondNewPass2"));
        assertTrue(passwordEncoder.matches("FirstNewPass1", user.getPassword()), "the second attempt changed nothing");
    }

    @Test
    void unknownExpiredUsedAndBlockedTokens_areAllRejectedWithTheSameError() {
        // unknown
        when(tokenRepository.findByTokenHash(PasswordResetService.hash("nope"))).thenReturn(Optional.empty());
        assertThrows(InvalidResetTokenException.class, () -> service.resetPassword("nope", "NewPass12345"));

        // expired
        PasswordResetToken expired = token("expired", LocalDateTime.now().minusMinutes(1), null);
        when(tokenRepository.findByTokenHash(PasswordResetService.hash("expired"))).thenReturn(Optional.of(expired));
        assertThrows(InvalidResetTokenException.class, () -> service.resetPassword("expired", "NewPass12345"));

        // already used
        PasswordResetToken used = token("used", LocalDateTime.now().plusMinutes(10), LocalDateTime.now().minusMinutes(1));
        when(tokenRepository.findByTokenHash(PasswordResetService.hash("used"))).thenReturn(Optional.of(used));
        assertThrows(InvalidResetTokenException.class, () -> service.resetPassword("used", "NewPass12345"));

        // valid token, but the account was blocked after the link was sent
        PasswordResetToken valid = token("valid", LocalDateTime.now().plusMinutes(10), null);
        when(tokenRepository.findByTokenHash(PasswordResetService.hash("valid"))).thenReturn(Optional.of(valid));
        user.setStatus(UserStatus.BANNED);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        assertThrows(InvalidResetTokenException.class, () -> service.resetPassword("valid", "NewPass12345"));

        assertTrue(passwordEncoder.matches("OldPassword1", user.getPassword()), "no attempt changed the password");
        verify(userRepository, never()).save(any());
    }

    private PasswordResetToken token(String raw, LocalDateTime expires, LocalDateTime usedAt) {
        PasswordResetToken t = new PasswordResetToken();
        t.setUserId("user-1");
        t.setTokenHash(PasswordResetService.hash(raw));
        t.setExpiresAt(expires);
        t.setUsedAt(usedAt);
        return t;
    }
}

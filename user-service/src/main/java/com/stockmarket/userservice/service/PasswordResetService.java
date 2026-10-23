package com.stockmarket.userservice.service;

import com.stockmarket.userservice.entity.PasswordResetToken;
import com.stockmarket.userservice.entity.User;
import com.stockmarket.userservice.exception.InvalidResetTokenException;
import com.stockmarket.userservice.repository.PasswordResetTokenRepository;
import com.stockmarket.userservice.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.concurrent.CompletableFuture;

/**
 * Forgot and reset password.
 * <ul>
 *   <li>{@link #requestReset} never reveals whether an email is registered: it always returns normally, and the
 *       email is sent in the background so response time does not leak it either.</li>
 *   <li>The token is 256 random bits; only its SHA-256 hash is stored. It works once and expires.</li>
 *   <li>A per-user cooldown stops the form being used to flood someone's inbox.</li>
 *   <li>A successful reset records {@code passwordChangedAt}; refresh tokens issued before it stop working.</li>
 * </ul>
 */
@Service
@Slf4j
public class PasswordResetService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final String baseUrl;
    private final long expiryMinutes;
    private final long cooldownSeconds;

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetTokenRepository tokenRepository,
                                PasswordEncoder passwordEncoder,
                                MailService mailService,
                                @Value("${app.base-url:http://localhost:3000}") String baseUrl,
                                @Value("${app.password-reset.expiry-minutes:30}") long expiryMinutes,
                                @Value("${app.password-reset.cooldown-seconds:60}") long cooldownSeconds) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.expiryMinutes = expiryMinutes;
        this.cooldownSeconds = cooldownSeconds;
    }

    @Transactional
    public void requestReset(String email) {
        userRepository.findByEmail(email).ifPresentOrElse(this::issueToken, () -> log.info("Password reset requested for an unknown email"));
    }

    private void issueToken(User user) {
        if (AccountPolicy.isBlocked(user.getStatus())) {
            log.info("Password reset requested for a blocked account: {}", user.getId());
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        boolean tooSoon = tokenRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId())
                .map(last -> last.getCreatedAt() != null && last.getCreatedAt().isAfter(now.minusSeconds(cooldownSeconds)))
                .orElse(false);
        if (tooSoon) {
            log.info("Password reset for {} skipped: asked again within the cooldown", user.getId());
            return;
        }

        tokenRepository.voidUnusedTokens(user.getId(), now); // only the newest link works

        byte[] raw = new byte[32];
        RANDOM.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

        PasswordResetToken entity = new PasswordResetToken();
        entity.setUserId(user.getId());
        entity.setTokenHash(hash(token));
        entity.setExpiresAt(now.plusMinutes(expiryMinutes));
        tokenRepository.save(entity);

        String to = user.getEmail();
        String body = "Hello " + user.getFirstName() + ",\n\n"
                + "We received a request to reset your Quantiva password. Open this link to choose a new one:\n\n"
                + baseUrl + "/reset-password?token=" + token + "\n\n"
                + "The link works once and expires in " + expiryMinutes + " minutes.\n"
                + "If you did not ask for this, you can ignore this email; your password stays the same.\n";
        // In the background, so the response time is the same whether or not the email exists.
        CompletableFuture.runAsync(() -> mailService.send(to, "Reset your Quantiva password", body));
        log.info("Password reset link issued for user: {}", user.getId());
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken stored = tokenRepository.findByTokenHash(hash(token)).orElseThrow(InvalidResetTokenException::new);
        LocalDateTime now = LocalDateTime.now();
        if (stored.getUsedAt() != null || stored.getExpiresAt().isBefore(now)) {
            throw new InvalidResetTokenException();
        }
        User user = userRepository.findById(stored.getUserId()).orElseThrow(InvalidResetTokenException::new);
        if (AccountPolicy.isBlocked(user.getStatus())) {
            throw new InvalidResetTokenException();
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(now);
        userRepository.save(user);
        tokenRepository.voidUnusedTokens(user.getId(), now); // this token and any other pending ones
        log.info("Password reset completed for user: {}", user.getId());
    }

    static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e); // SHA-256 is always present
        }
    }
}

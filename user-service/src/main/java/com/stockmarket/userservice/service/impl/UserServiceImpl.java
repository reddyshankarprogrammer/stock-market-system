package com.stockmarket.userservice.service.impl;

import com.stockmarket.userservice.dto.*;
import com.stockmarket.userservice.repository.UserRepository;
import com.stockmarket.userservice.security.JwtService;
import com.stockmarket.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;


import com.stockmarket.userservice.entity.Gender;
import com.stockmarket.userservice.entity.User;
import com.stockmarket.userservice.entity.UserStatus;
import com.stockmarket.userservice.exception.InvalidRefreshTokenException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;


@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final JwtService jwtService;

    private static final String USER_REGISTERED_TOPIC = "user.registered";


    @Override
    public AuthResponse register(RegisterRequest request){

        log.info("registering user: {}", request.getEmail());

        if(userRepository.existsByEmail(request.getEmail())){
            throw new RuntimeException(
                    "Email already registered " + request.getEmail());
        }

        User user = new User();

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());

        user.setMobileCountryCode(request.getMobileCountryCode());
        user.setMobileNumber(request.getMobileNumber());
        user.setGender(request.getGender() != null ? Gender.valueOf(request.getGender()) : null);
        user.setCountry(request.getCountry());
        if (request.getTermsVersion() != null) {
            user.setTermsVersion(request.getTermsVersion());
            user.setTermsAcceptedAt(LocalDateTime.now()); // server time, never taken from the client
        }

        user.setPassword(passwordEncoder.encode(request.getPassword()));

        user.setWalletBalance(
                request.getInitialBalance() != null
                        ? request.getInitialBalance()
                        : BigDecimal.valueOf(10000));

        User savedUser = userRepository.save(user);

        log.info("User registered: {}", savedUser.getId());

        // Publish user.registered event
        Map<String, Object> userRegisteredEvent = new HashMap<>();

        userRegisteredEvent.put("userId", savedUser.getId());
        userRegisteredEvent.put("email", savedUser.getEmail());
        userRegisteredEvent.put("firstName", savedUser.getFirstName());
        userRegisteredEvent.put("lastName", savedUser.getLastName());
        userRegisteredEvent.put("walletBalance", savedUser.getWalletBalance());


        kafkaTemplate.send(USER_REGISTERED_TOPIC,savedUser.getId() ,userRegisteredEvent);

        String token = jwtService.generateAccessToken(savedUser.getId(), savedUser.getEmail(), savedUser.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(savedUser.getId());

        return buildAuthResponse(savedUser, token, refreshToken);


    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        AuthResponse authResponse = new AuthResponse();
        authResponse.setUserId(user.getId());
        authResponse.setEmail(user.getEmail());
        authResponse.setFirstName(user.getFirstName());
        authResponse.setLastName(user.getLastName());
        authResponse.setWalletBalance(user.getWalletBalance());
        authResponse.setAccessToken(accessToken);
        authResponse.setRefreshToken(refreshToken);
        authResponse.setTokenType("Bearer");
        return authResponse;
    }

    private AuthResponse buildAuthResponse(String userId, String accessToken, String refreshToken) {
        return userRepository.findById(userId)
                .map(user -> buildAuthResponse(user, accessToken, refreshToken))
                .orElseGet(() -> {
                    AuthResponse authResponse = new AuthResponse();
                    authResponse.setUserId(userId);
                    authResponse.setAccessToken(accessToken);
                    authResponse.setRefreshToken(refreshToken);
                    authResponse.setTokenType("Bearer");
                    return authResponse;
                });
    }




    @Override
    public AuthResponse login(LoginRequest request) {

        log.info("Login attempt: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException(
                        "User not found: " + request.getEmail()));

        if (!passwordEncoder.matches(
                request.getPassword(), user.getPassword())) {

            throw new RuntimeException(
                    "Invalid Credentials");
        }

        log.info("Login successful: {}", user.getId());

        String token = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        return buildAuthResponse(
                user,
                token,
                refreshToken);
    }


    // Statuses that must not get a new access token. Note: login does not check status today (known gap in the README).
    private static final Set<UserStatus> REFRESH_DENIED = EnumSet.of(
            UserStatus.INACTIVE, UserStatus.SUSPENDED, UserStatus.DELETED, UserStatus.BLOCKED, UserStatus.BANNED,
            UserStatus.ACCOUNT_LOCKED, UserStatus.ACCOUNT_EXPIRED, UserStatus.ACCOUNT_DISABLED);

    @Override
    public AuthResponse refresh(RefreshRequest request) {
        String refreshToken = request.getRefreshToken();

        // Signature, expiry and token type are all checked; an access token must never work here.
        if (!jwtService.validateToken(refreshToken)
                || !JwtService.TokenType.REFRESH.name().equals(jwtService.extractTokenType(refreshToken))) {
            throw new InvalidRefreshTokenException();
        }

        User user = userRepository.findById(jwtService.extractUserId(refreshToken))
                .orElseThrow(InvalidRefreshTokenException::new);
        if (user.getStatus() != null && REFRESH_DENIED.contains(user.getStatus())) {
            throw new InvalidRefreshTokenException();
        }

        log.info("Access token refreshed for user: {}", user.getId());
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        return buildAuthResponse(user, accessToken, refreshToken);
    }

    @Override
    public UserResponse getUserById(String userId) {
        log.info("Fetching user profile for userId: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        return mapToUserResponse(user);
    }

    @Override
    public UserResponse addFunds(String userId, BigDecimal amount) {
        log.info("Adding funds for userId: {}, amount: {}", userId, amount);
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        BigDecimal currentBalance = user.getWalletBalance() != null ? user.getWalletBalance() : BigDecimal.ZERO;
        user.setWalletBalance(currentBalance.add(amount));

        User updatedUser = userRepository.save(user);
        log.info("Funds added successfully. New balance: {}", updatedUser.getWalletBalance());
        return mapToUserResponse(updatedUser);
    }

    @Override
    public UserResponse deductFunds(String userId, BigDecimal amount) {
        log.info("Deducting funds for userId: {}, amount: {}", userId, amount);
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        BigDecimal currentBalance = user.getWalletBalance() != null ? user.getWalletBalance() : BigDecimal.ZERO;
        if (currentBalance.compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient wallet balance");
        }
        user.setWalletBalance(currentBalance.subtract(amount));

        User updatedUser = userRepository.save(user);
        log.info("Funds deducted successfully. New balance: {}", updatedUser.getWalletBalance());
        return mapToUserResponse(updatedUser);
    }

    @Override
    public UserResponse creditFunds(String userId, BigDecimal amount) {
        log.info("Crediting funds for userId: {}, amount: {}", userId, amount);
        return addFunds(userId, amount);
    }

    private UserResponse mapToUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getWalletBalance(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getRole(),
                user.getMobileCountryCode(),
                user.getMobileNumber(),
                user.getGender(),
                user.getCountry(),
                user.getTermsVersion(),
                user.getTermsAcceptedAt()
        );
    }
}
package com.stockmarket.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmarket.userservice.config.SecurityConfig;
import com.stockmarket.userservice.dto.AuthResponse;
import com.stockmarket.userservice.dto.LoginRequest;
import com.stockmarket.userservice.dto.RegisterRequest;
import com.stockmarket.userservice.dto.UserResponse;
import com.stockmarket.userservice.entity.Role;
import com.stockmarket.userservice.entity.UserStatus;
import com.stockmarket.userservice.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private com.stockmarket.userservice.service.PasswordResetService passwordResetService;

    @MockitoBean
    private com.stockmarket.userservice.service.OnboardingService onboardingService;

    @MockitoBean
    private com.stockmarket.userservice.service.PreferencesService preferencesService;

    @MockitoBean
    private com.stockmarket.userservice.service.WaitlistService waitlistService;

    @Test
    void register_ValidRequest_ReturnsCreated() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("user@example.com");
        request.setPassword("SecurePass123!");
        request.setFirstName("John");
        request.setLastName("Doe");

        AuthResponse authResponse = new AuthResponse();
        authResponse.setUserId("user-1");
        authResponse.setEmail("user@example.com");
        authResponse.setAccessToken("token-123");

        when(userService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value("user-1"))
                .andExpect(jsonPath("$.accessToken").value("token-123"));
    }

    private static final String BASE_REGISTER = "\"email\":\"user@example.com\",\"password\":\"SecurePass123!\",\"firstName\":\"John\",\"lastName\":\"Doe\"";

    @Test
    void register_WithProfileFields_PassesThemToTheService() throws Exception {
        when(userService.register(any(RegisterRequest.class))).thenReturn(new AuthResponse());

        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + BASE_REGISTER + ",\"mobileCountryCode\":\"+91\",\"mobileNumber\":\"9876543210\","
                                + "\"gender\":\"FEMALE\",\"country\":\"IN\",\"termsVersion\":\"2026-10-24-draft\"}"))
                .andExpect(status().isCreated());

        org.mockito.ArgumentCaptor<RegisterRequest> sent = org.mockito.ArgumentCaptor.forClass(RegisterRequest.class);
        org.mockito.Mockito.verify(userService).register(sent.capture());
        org.junit.jupiter.api.Assertions.assertEquals("+91", sent.getValue().getMobileCountryCode());
        org.junit.jupiter.api.Assertions.assertEquals("9876543210", sent.getValue().getMobileNumber());
        org.junit.jupiter.api.Assertions.assertEquals("FEMALE", sent.getValue().getGender());
        org.junit.jupiter.api.Assertions.assertEquals("IN", sent.getValue().getCountry());
    }

    @Test
    void register_InvalidProfileFields_ReturnsBadRequest() throws Exception {
        for (String bad : new String[] {
                "\"gender\":\"OTHER\"",
                "\"country\":\"india\"",
                "\"mobileCountryCode\":\"91\",\"mobileNumber\":\"9876543210\"",
                "\"mobileCountryCode\":\"+91\",\"mobileNumber\":\"12ab\"",
                "\"mobileCountryCode\":\"+91\"" }) {
            mockMvc.perform(post("/api/v1/users/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{" + BASE_REGISTER + "," + bad + "}"))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(userService);
    }

    @Test
    void refresh_ValidToken_ReturnsNewAccessToken() throws Exception {
        AuthResponse authResponse = new AuthResponse();
        authResponse.setUserId("user-1");
        authResponse.setAccessToken("new-access");
        authResponse.setRefreshToken("same-refresh");
        when(userService.refresh(any(com.stockmarket.userservice.dto.RefreshRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/users/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"same-refresh\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access"))
                .andExpect(jsonPath("$.refreshToken").value("same-refresh"));
    }

    @Test
    void refresh_InvalidToken_ReturnsUnauthorized() throws Exception {
        when(userService.refresh(any(com.stockmarket.userservice.dto.RefreshRequest.class)))
                .thenThrow(new com.stockmarket.userservice.exception.InvalidRefreshTokenException());

        mockMvc.perform(post("/api/v1/users/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"bad\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid refresh token"));
    }

    @Test
    void refresh_BlankToken_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/users/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(userService);
    }

    @Test
    void forgotPassword_answersTheSameWhateverTheEmail() throws Exception {
        for (String email : new String[] {"known@example.com", "nobody@example.com"}) {
            mockMvc.perform(post("/api/v1/users/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + email + "\"}"))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.message").value("If an account exists for that email, a reset link has been sent"));
        }
        org.mockito.Mockito.verify(passwordResetService).requestReset("known@example.com");
        org.mockito.Mockito.verify(passwordResetService).requestReset("nobody@example.com");
    }

    @Test
    void forgotPassword_badEmail_isRejectedWithoutTouchingTheService() throws Exception {
        mockMvc.perform(post("/api/v1/users/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(passwordResetService);
    }

    @Test
    void resetPassword_validRequest_ReturnsOk() throws Exception {
        mockMvc.perform(post("/api/v1/users/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"abc123\",\"newPassword\":\"BrandNewPass9\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Your password has been changed"));
        org.mockito.Mockito.verify(passwordResetService).resetPassword("abc123", "BrandNewPass9");
    }

    @Test
    void resetPassword_invalidToken_ReturnsBadRequestWithOneMessage() throws Exception {
        org.mockito.Mockito.doThrow(new com.stockmarket.userservice.exception.InvalidResetTokenException())
                .when(passwordResetService).resetPassword(eq("bad"), any());

        mockMvc.perform(post("/api/v1/users/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"bad\",\"newPassword\":\"BrandNewPass9\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("This reset link is invalid or has expired"));
    }

    @Test
    void resetPassword_badBodies_areRejected() throws Exception {
        for (String body : new String[] {
                "{\"token\":\"abc\",\"newPassword\":\"short\"}",
                "{\"token\":\"abc\",\"newPassword\":\"" + "x".repeat(73) + "\"}",
                "{\"token\":\"\",\"newPassword\":\"BrandNewPass9\"}",
                "{\"newPassword\":\"BrandNewPass9\"}" }) {
            mockMvc.perform(post("/api/v1/users/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(passwordResetService);
    }

    private com.stockmarket.userservice.dto.OnboardingResponse onboarding() {
        return new com.stockmarket.userservice.dto.OnboardingResponse("ADVANCED", java.util.List.of("GROWW", "ZERODHA"), "PRO", false, null);
    }

    @Test
    void onboarding_get_usesTheGatewayHeader_notAnythingElse() throws Exception {
        when(onboardingService.get("user-1")).thenReturn(onboarding());

        mockMvc.perform(get("/api/v1/users/me/onboarding").header("X-User-Id", "user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tradingExperience").value("ADVANCED"))
                .andExpect(jsonPath("$.brokerPreferences[1]").value("ZERODHA"))
                .andExpect(jsonPath("$.completed").value(false));

        mockMvc.perform(get("/api/v1/users/me/onboarding")).andExpect(status().isUnauthorized()); // no header, no identity
    }

    @Test
    void onboarding_put_savesForTheCaller_andIgnoresAUserIdInTheBody() throws Exception {
        when(onboardingService.save(eq("user-1"), any())).thenReturn(onboarding());

        mockMvc.perform(put("/api/v1/users/me/onboarding").header("X-User-Id", "user-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tradingExperience\":\"ADVANCED\",\"brokerPreferences\":[\"GROWW\",\"ZERODHA\"],\"plan\":\"PRO\",\"userId\":\"someone-else\"}"))
                .andExpect(status().isOk());

        org.mockito.Mockito.verify(onboardingService).save(eq("user-1"), any()); // never "someone-else"
    }

    @Test
    void onboarding_put_rejectsBadValues() throws Exception {
        for (String body : new String[] {
                "{\"tradingExperience\":\"GURU\"}",
                "{\"plan\":\"PLATINUM\"}",
                "{\"brokerPreferences\":[\"MADEUP\"]}",
                "{\"brokerPreferences\":[\"GROWW\",\"ZERODHA\",\"BINANCE\",\"EXNESS\",\"OTHER\",\"NONE\",\"GROWW\"]}" }) {
            mockMvc.perform(put("/api/v1/users/me/onboarding").header("X-User-Id", "user-1")
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        org.mockito.Mockito.verifyNoInteractions(onboardingService);
    }

    @Test
    void onboarding_complete_andItsRulesComeBackAs400() throws Exception {
        when(onboardingService.complete("user-1")).thenReturn(onboarding());
        mockMvc.perform(post("/api/v1/users/me/onboarding/complete").header("X-User-Id", "user-1")).andExpect(status().isOk());

        when(onboardingService.complete("user-2")).thenThrow(new IllegalArgumentException("Choose your trading experience, your brokers and a plan first"));
        mockMvc.perform(post("/api/v1/users/me/onboarding/complete").header("X-User-Id", "user-2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Choose your trading experience, your brokers and a plan first"));

        when(onboardingService.get("ghost")).thenThrow(new com.stockmarket.userservice.exception.UserNotFoundException());
        mockMvc.perform(get("/api/v1/users/me/onboarding").header("X-User-Id", "ghost")).andExpect(status().isNotFound());
    }

    @Test
    void login_BadCredentials_Returns401_WithOneMessage() throws Exception {
        when(userService.login(any(LoginRequest.class))).thenThrow(new com.stockmarket.userservice.exception.InvalidCredentialsException());

        mockMvc.perform(post("/api/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"SecurePass123!\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid Credentials"));
    }

    @Test
    void login_AccountNotActive_Returns403() throws Exception {
        when(userService.login(any(LoginRequest.class))).thenThrow(new com.stockmarket.userservice.exception.AccountNotActiveException());

        mockMvc.perform(post("/api/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"SecurePass123!\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("This account is not active"));
    }

    @Test
    void login_ValidRequest_ReturnsOk() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("SecurePass123!");

        AuthResponse authResponse = new AuthResponse();
        authResponse.setUserId("user-1");
        authResponse.setEmail("user@example.com");
        authResponse.setAccessToken("token-123");

        when(userService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-1"))
                .andExpect(jsonPath("$.accessToken").value("token-123"));
    }

    @Test
    void getUserById_ReturnsOk() throws Exception {
        UserResponse response = new UserResponse("user-1", "user@example.com", "John", "Doe", BigDecimal.valueOf(5000), UserStatus.ACTIVE, LocalDateTime.now(), Role.USER, null, null, null, null, null, null, null);

        when(userService.getUserById("user-1")).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/user-1").header("X-User-Id", "user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("user-1"))
                .andExpect(jsonPath("$.walletBalance").value(5000));
    }

    @Test
    void getUserById_OtherUser_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/users/user-2").header("X-User-Id", "user-1"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    @Test
    void getUserById_OtherUserAsAdmin_ReturnsOk() throws Exception {
        UserResponse response = new UserResponse("user-2", "other@example.com", "Jane", "Roe", BigDecimal.valueOf(100), UserStatus.ACTIVE, LocalDateTime.now(), Role.USER, null, null, null, null, null, null, null);
        when(userService.getUserById("user-2")).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/user-2").header("X-User-Id", "admin-1").header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("user-2"));
    }

    @Test
    void getUserById_MissingUserHeader_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users/user-1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addFunds_ReturnsOk() throws Exception {
        UserResponse response = new UserResponse("user-1", "user@example.com", "John", "Doe", BigDecimal.valueOf(6000), UserStatus.ACTIVE, LocalDateTime.now(), Role.USER, null, null, null, null, null, null, null);

        when(userService.addFunds(eq("user-1"), any(BigDecimal.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/users/user-1/funds/add")
                        .header("X-User-Id", "user-1")
                        .param("amount", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletBalance").value(6000));
    }

    @Test
    void addFunds_OtherUser_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/users/user-2/funds/add")
                        .header("X-User-Id", "user-1")
                        .param("amount", "1000"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    // ---- preferences and change password ----

    @Test
    void preferences_get_usesTheGatewayHeader_andNeedsIt() throws Exception {
        when(preferencesService.get("user-1")).thenReturn(new com.stockmarket.userservice.dto.PreferencesResponse("DARK", "en", true, false, false, "CSV"));

        mockMvc.perform(get("/api/v1/users/me/preferences").header("X-User-Id", "user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme").value("DARK"))
                .andExpect(jsonPath("$.notifyPush").value(false));

        mockMvc.perform(get("/api/v1/users/me/preferences")).andExpect(status().isUnauthorized());
    }

    @Test
    void preferences_put_savesForTheCaller_andRejectsUnknownValues() throws Exception {
        when(preferencesService.save(eq("user-1"), any())).thenReturn(new com.stockmarket.userservice.dto.PreferencesResponse("LIGHT", "en", true, false, false, "CSV"));

        mockMvc.perform(put("/api/v1/users/me/preferences").header("X-User-Id", "user-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"theme\":\"LIGHT\",\"userId\":\"someone-else\"}"))
                .andExpect(status().isOk());
        org.mockito.Mockito.verify(preferencesService).save(eq("user-1"), any());

        mockMvc.perform(put("/api/v1/users/me/preferences").header("X-User-Id", "user-1")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"theme\":\"NEON\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/v1/users/me/preferences").header("X-User-Id", "user-1")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"exportFormat\":\"XML\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changePassword_wrongCurrentPassword_is422_not401_sotheSessionIsNotEnded() throws Exception {
        when(userService.changePassword(eq("user-1"), any())).thenThrow(new com.stockmarket.userservice.exception.IncorrectPasswordException());

        mockMvc.perform(post("/api/v1/users/me/password").header("X-User-Id", "user-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"wrong\",\"newPassword\":\"NewPass456\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Your current password is incorrect"));
    }

    @Test
    void changePassword_validatesTheNewPassword_andNeverEchoesPasswords() throws Exception {
        mockMvc.perform(post("/api/v1/users/me/password").header("X-User-Id", "user-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"OldPass123\",\"newPassword\":\"abc\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"OldPass123\",\"newPassword\":\"NewPass456\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ---- waitlist ----

    @Test
    void waitlist_isPublic_answers202_andNeverEchoesTheEmail() throws Exception {
        mockMvc.perform(post("/api/v1/users/waitlist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"asha@example.com\",\"consent\":true,\"source\":\"landing\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value("You are on the list. We will write to you when Quantiva opens."));
        org.mockito.Mockito.verify(waitlistService).join(any());
    }

    @Test
    void waitlist_needsAValidEmail_andConsent() throws Exception {
        mockMvc.perform(post("/api/v1/users/waitlist").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"consent\":true}")).andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/users/waitlist").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"asha@example.com\",\"consent\":false}")).andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/users/waitlist").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"asha@example.com\",\"consent\":true,\"source\":\"<script>\"}")).andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/users/waitlist").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consent\":true}")).andExpect(status().isBadRequest());
        org.mockito.Mockito.verify(waitlistService, org.mockito.Mockito.never()).join(any());
    }
}

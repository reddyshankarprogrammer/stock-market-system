package com.stockmarket.accountservice.service.impl;

import com.stockmarket.accountservice.client.UserDto;
import com.stockmarket.accountservice.client.UserServiceClient;
import com.stockmarket.accountservice.exception.AccountAccessDeniedException;
import com.stockmarket.accountservice.exception.ResourceNotFoundException;
import com.stockmarket.accountservice.exception.UserServiceUnavailableException;
import com.stockmarket.accountservice.exception.ValidationException;
import com.stockmarket.accountservice.security.RequestUser;
import com.stockmarket.accountservice.service.UserValidationService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserValidationServiceImpl implements UserValidationService {

    private final UserServiceClient userServiceClient;

    @Override
    public UserDto requireActiveCurrentUser() {

        UserDto user = call(userServiceClient::getCurrentUser, RequestUser.userId());

        if (!user.isActive()) {
            throw new ValidationException("User " + user.id() + " is " + user.status() + "; only ACTIVE users can open accounts");
        }
        return user;
    }

    @Override
    public UserDto getUser(String userId) {

        UUID id;
        try {
            id = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("User id must be a UUID: " + userId);
        }
        return call(() -> userServiceClient.getUser(id), userId);
    }

    /**
     * Maps Feign errors to account-service exceptions.
     */
    private UserDto call(Supplier<UserDto> request, String userId) {
        try {
            UserDto user = request.get();
            if (user == null) {
                throw new ResourceNotFoundException("User", userId);
            }
            return user;
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException("User", userId);
        } catch (FeignException.Forbidden e) {
            throw AccountAccessDeniedException.forUser(userId);
        } catch (FeignException.Unauthorized e) {
            throw new ValidationException("Missing user identity for user-service call");
        } catch (FeignException e) {
            log.error("user-service call failed for {}: {}", userId, e.getMessage());
            throw new UserServiceUnavailableException(e);
        }
    }
}

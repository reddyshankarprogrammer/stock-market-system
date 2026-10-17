package com.stockmarket.accountservice.service;

import com.stockmarket.accountservice.client.UserDto;
import com.stockmarket.accountservice.exception.ResourceNotFoundException;
import com.stockmarket.accountservice.exception.UserServiceUnavailableException;
import com.stockmarket.accountservice.exception.ValidationException;

/**
 * Checks users against user-service before account-service acts for them.
 */
public interface UserValidationService {

    /**
     * The calling user (from the forwarded X-User-Id), required to exist and be ACTIVE.
     *
     * @throws ResourceNotFoundException        if user-service does not know the user
     * @throws ValidationException              if the user is not ACTIVE
     * @throws UserServiceUnavailableException  if user-service cannot be reached
     */
    UserDto requireActiveCurrentUser();

    /**
     * Any user by id (allowed for the user themselves or an ADMIN caller).
     */
    UserDto getUser(String userId);
}

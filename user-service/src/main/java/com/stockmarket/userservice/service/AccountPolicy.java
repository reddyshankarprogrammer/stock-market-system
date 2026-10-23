package com.stockmarket.userservice.service;

import com.stockmarket.userservice.entity.UserStatus;

import java.util.EnumSet;
import java.util.Set;

/**
 * Which account statuses must not get new credentials. Used by refresh and password reset. A null status (rows from
 * before the column existed) counts as normal. Login does not use this yet (known gap in the README).
 */
public final class AccountPolicy {

    private static final Set<UserStatus> BLOCKED = EnumSet.of(
            UserStatus.INACTIVE, UserStatus.SUSPENDED, UserStatus.DELETED, UserStatus.BLOCKED, UserStatus.BANNED,
            UserStatus.ACCOUNT_LOCKED, UserStatus.ACCOUNT_EXPIRED, UserStatus.ACCOUNT_DISABLED);

    private AccountPolicy() {
    }

    public static boolean isBlocked(UserStatus status) {
        return status != null && BLOCKED.contains(status);
    }
}

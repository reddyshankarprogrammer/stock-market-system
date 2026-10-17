package com.stockmarket.userservice.entity;

/**
 * Authorization role of a user, carried in the JWT as the {@code role} claim.
 */
public enum Role {

    /**
     * Regular trader: may only access their own data.
     */
    USER,

    /**
     * Back-office user: may read other users' accounts and call admin endpoints
     * (dividend and interest posting in account-service).
     */
    ADMIN
}

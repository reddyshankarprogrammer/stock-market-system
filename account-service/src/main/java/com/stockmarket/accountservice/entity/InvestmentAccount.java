package com.stockmarket.accountservice.entity;

import com.stockmarket.accountservice.enums.AccountStatus;
import com.stockmarket.accountservice.enums.AccountType;
import com.stockmarket.accountservice.enums.BrokerName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A user's investment (brokerage) account.
 *
 * The {@code balance} is the cash available for trading. It is only ever changed by
 * {@code LedgerService}, so it always equals the {@code balanceAfter} of the account's
 * latest ledger entry.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "investment_accounts",
        indexes = {
                @Index(name = "idx_investment_accounts_user_id", columnList = "user_id"),
                @Index(name = "idx_investment_accounts_status", columnList = "status"),
                @Index(name = "idx_investment_accounts_broker", columnList = "broker_name"),
                @Index(name = "idx_investment_accounts_user_broker", columnList = "user_id, broker_name")
        }
)
public class InvestmentAccount extends BaseEntity {

    /**
     * Human-readable account number, for example {@code INV482019375561}.
     */
    @NotBlank
    @Size(max = 20)
    @Column(nullable = false, unique = true, length = 20)
    private String accountNumber;

    /**
     * Owner, the user-service user id (taken from {@code X-User-Id}).
     */
    @NotBlank
    @Size(max = 64)
    @Column(nullable = false, length = 64)
    private String userId;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String accountName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private AccountType accountType;

    /**
     * Where the account is held. PLATFORM/PAPER trade here; the others are tracking accounts.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private BrokerName brokerName = BrokerName.PLATFORM;

    /**
     * ISO 4217 currency code of the balance.
     */
    @NotBlank
    @Pattern(regexp = "[A-Z]{3}")
    @Column(nullable = false, length = 3)
    private String currency;

    @Size(max = 500)
    @Column(length = 500)
    private String remarks;

    @NotNull
    @PositiveOrZero
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    /**
     * When the account was closed; null while open.
     */
    private LocalDateTime closedAt;

    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }

    /**
     * True for tracking accounts, whose trades come from manual entry or statement import.
     */
    public boolean isTracking() {
        return !brokerName.isPlatformTraded();
    }
}

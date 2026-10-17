package com.stockmarket.accountservice.entity;

import com.stockmarket.accountservice.enums.CalculationType;
import com.stockmarket.accountservice.enums.FeeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

/**
 * Fee schedule entry used by the fee engine.
 *
 * <ul>
 *   <li>FIXED: the fee is {@code value}.</li>
 *   <li>PERCENTAGE: the fee is {@code value}% of the base amount, then clamped to
 *       {@code minAmount} / {@code maxAmount} when they are set.</li>
 * </ul>
 * Only one entry per {@link FeeType} should be active at a time.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "fee_master",
        indexes = @Index(name = "idx_fee_master_type_active", columnList = "fee_type, active")
)
public class FeeMaster extends BaseEntity {

    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private FeeType feeType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private CalculationType calculationType;

    /**
     * Fixed amount, or percentage (2.5 means 2.5%).
     */
    @NotNull
    @PositiveOrZero
    @Column(name = "fee_value", nullable = false, precision = 19, scale = 6)
    private BigDecimal value;

    @PositiveOrZero
    @Column(precision = 19, scale = 2)
    private BigDecimal minAmount;

    @PositiveOrZero
    @Column(precision = 19, scale = 2)
    private BigDecimal maxAmount;

    @Column(nullable = false)
    private boolean active;

    @Size(max = 255)
    @Column(length = 255)
    private String description;
}

package com.stockmarket.accountservice.entity;

import com.stockmarket.accountservice.enums.CalculationType;
import com.stockmarket.accountservice.enums.TaxType;
import com.stockmarket.accountservice.enums.TradeSide;
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
 * Tax rule used by the tax engine.
 *
 * {@code appliesToSide} restricts a TRADE_TAX rule to BUY or SELL trades;
 * null means it applies to both.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "tax_master",
        indexes = @Index(name = "idx_tax_master_type_active", columnList = "tax_type, active")
)
public class TaxMaster extends BaseEntity {

    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private TaxType taxType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private CalculationType calculationType;

    /**
     * Fixed amount, or percentage (10 means 10%).
     */
    @NotNull
    @PositiveOrZero
    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal rate;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 10)
    private TradeSide appliesToSide;

    @Column(nullable = false)
    private boolean active;

    @Size(max = 255)
    @Column(length = 255)
    private String description;
}

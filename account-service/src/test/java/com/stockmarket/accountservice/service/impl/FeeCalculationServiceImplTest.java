package com.stockmarket.accountservice.service.impl;

import com.stockmarket.accountservice.entity.FeeMaster;
import com.stockmarket.accountservice.enums.CalculationType;
import com.stockmarket.accountservice.enums.FeeType;
import com.stockmarket.accountservice.repository.FeeMasterRepository;
import com.stockmarket.accountservice.service.model.FeeBreakdown;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class FeeCalculationServiceImplTest {

    @Mock
    private FeeMasterRepository feeMasterRepository;

    private FeeCalculationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FeeCalculationServiceImpl(feeMasterRepository, null, null, null);
        lenient().when(feeMasterRepository.findFirstByFeeTypeAndActiveTrue(any())).thenReturn(Optional.empty());
        stub(FeeType.BROKERAGE, CalculationType.PERCENTAGE, "0.10", "1.00", "25.00");
        stub(FeeType.PLATFORM, CalculationType.FIXED, "0.50", null, null);
        stub(FeeType.EXCHANGE, CalculationType.PERCENTAGE, "0.0035", null, null);
    }

    @Test
    void percentageFeeIsAppliedToTheBase() {
        assertThat(service.calculateBrokerage(new BigDecimal("5000"))).isEqualByComparingTo("5.00");
    }

    @Test
    void percentageFeeIsRaisedToTheMinimum() {
        assertThat(service.calculateBrokerage(new BigDecimal("100"))).isEqualByComparingTo("1.00");
    }

    @Test
    void percentageFeeIsCappedAtTheMaximum() {
        assertThat(service.calculateBrokerage(new BigDecimal("100000"))).isEqualByComparingTo("25.00");
    }

    @Test
    void fixedFeeIgnoresTheBase() {
        assertThat(service.calculatePlatformFee(new BigDecimal("123456.78"))).isEqualByComparingTo("0.50");
    }

    @Test
    void feesAreRoundedHalfUpToCents() {
        // 1000 x 0.0035% = 0.035 -> 0.04
        assertThat(service.calculateExchangeFee(new BigDecimal("1000"))).isEqualByComparingTo("0.04");
    }

    @Test
    void feeTypeWithoutActiveScheduleCostsNothing() {
        assertThat(service.calculateWithdrawalFee(new BigDecimal("500"))).isEqualByComparingTo("0.00");
        assertThat(service.calculateWithdrawalFees(new BigDecimal("500")).charges()).isEmpty();
    }

    @Test
    void totalFeesSumBrokeragePlatformAndExchange() {
        FeeBreakdown breakdown = service.calculateTotalFees(new BigDecimal("1000.00"));

        assertThat(breakdown.charges()).hasSize(3);
        assertThat(breakdown.total()).isEqualByComparingTo("1.54");
        assertThat(breakdown.baseAmount()).isEqualByComparingTo("1000.00");
    }

    private void stub(FeeType type, CalculationType calculation, String value, String min, String max) {
        FeeMaster master = FeeMaster.builder()
                .code(type + "_TEST")
                .feeType(type)
                .calculationType(calculation)
                .value(new BigDecimal(value))
                .minAmount(min == null ? null : new BigDecimal(min))
                .maxAmount(max == null ? null : new BigDecimal(max))
                .active(true)
                .build();
        lenient().when(feeMasterRepository.findFirstByFeeTypeAndActiveTrue(type)).thenReturn(Optional.of(master));
    }
}

package com.computedeliver.orders.order;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PricingTest {

    @Test
    void tenPercentOfTenUnitsAtOneHundred() {
        long gross = 10 * 10_000L;
        assertThat(Pricing.discountCents(gross, 1000)).isEqualTo(10_000L);
        assertThat(Pricing.netCents(gross, 1000)).isEqualTo(90_000L);
    }

    @Test
    void zeroDiscountLeavesNetEqualToGross() {
        assertThat(Pricing.discountCents(123_457L, 0)).isZero();
        assertThat(Pricing.netCents(123_457L, 0)).isEqualTo(123_457L);
    }

    @Test
    void fractionalCentsRoundHalfUp() {
        // 10% of 1005 = 100.5 -> 101
        assertThat(Pricing.discountCents(1005, 1000)).isEqualTo(101);
        assertThat(Pricing.netCents(1005, 1000)).isEqualTo(904);
        // 10% of 1004 = 100.4 -> 100
        assertThat(Pricing.discountCents(1004, 1000)).isEqualTo(100);
        // 15% of 1003 = 150.45 -> 150
        assertThat(Pricing.discountCents(1003, 1500)).isEqualTo(150);
    }
}

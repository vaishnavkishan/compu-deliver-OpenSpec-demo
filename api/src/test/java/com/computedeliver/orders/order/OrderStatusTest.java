package com.computedeliver.orders.order;

import static com.computedeliver.orders.order.OrderStatus.*;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OrderStatusTest {

    @Test
    void advancesExactlyOneStepForward() {
        assertThat(CONFIRMED.canAdvanceTo(PROCESSING)).isTrue();
        assertThat(SUBMITTED.next()).contains(CONFIRMED);
        assertThat(SHIPPED.next()).contains(DELIVERED);
    }

    @Test
    void skippingIsRejected() {
        assertThat(SUBMITTED.canAdvanceTo(SHIPPED)).isFalse();
    }

    @Test
    void backwardIsRejected() {
        assertThat(SHIPPED.canAdvanceTo(PROCESSING)).isFalse();
    }

    @Test
    void terminalStatesHaveNoSuccessor() {
        assertThat(DELIVERED.next()).isEmpty();
        assertThat(CANCELLED.next()).isEmpty();
        assertThat(DELIVERED.canAdvanceTo(CANCELLED)).isFalse();
        assertThat(SHIPPED.canAdvanceTo(CANCELLED)).isFalse();
    }

    @Test
    void onlySubmittedAndConfirmedAreCancellable() {
        assertThat(SUBMITTED.isCancellable()).isTrue();
        assertThat(CONFIRMED.isCancellable()).isTrue();
        assertThat(PROCESSING.isCancellable()).isFalse();
        assertThat(CANCELLED.isCancellable()).isFalse();
    }
}

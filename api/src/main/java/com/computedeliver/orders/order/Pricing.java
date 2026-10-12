package com.computedeliver.orders.order;

/** Money is integer cents and discounts are integer basis points; no floating point anywhere. */
public final class Pricing {

    private Pricing() {
    }

    /** round-half-up(gross * discountBp / 10000) */
    public static long discountCents(long grossCents, int discountBp) {
        return (Math.multiplyExact(grossCents, (long) discountBp) + 5_000L) / 10_000L;
    }

    public static long netCents(long grossCents, int discountBp) {
        return grossCents - discountCents(grossCents, discountBp);
    }
}

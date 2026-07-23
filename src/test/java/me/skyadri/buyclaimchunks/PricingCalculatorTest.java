package me.skyadri.buyclaimchunks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PricingCalculatorTest {
    private static final long BASE_PRICE = 4;
    private static final double GROWTH_FACTOR = 3.45D;
    private static final double EXPONENT = 0.5D;

    @Test
    void defaultCurveMatchesReferencePrices() {
        assertEquals(4, PricingCalculator.priceForClaim(1, BASE_PRICE, GROWTH_FACTOR, EXPONENT));
        assertEquals(11, PricingCalculator.priceForClaim(10, BASE_PRICE, GROWTH_FACTOR, EXPONENT));
        assertEquals(25, PricingCalculator.priceForClaim(50, BASE_PRICE, GROWTH_FACTOR, EXPONENT));
        assertEquals(35, PricingCalculator.priceForClaim(100, BASE_PRICE, GROWTH_FACTOR, EXPONENT));
    }

    @Test
    void batchPriceSumsEveryClaimInTheRange() {
        long expected = PricingCalculator.priceForClaim(9, BASE_PRICE, GROWTH_FACTOR, EXPONENT)
                + PricingCalculator.priceForClaim(10, BASE_PRICE, GROWTH_FACTOR, EXPONENT)
                + PricingCalculator.priceForClaim(11, BASE_PRICE, GROWTH_FACTOR, EXPONENT);

        assertEquals(expected, PricingCalculator.totalPrice(8, 3, BASE_PRICE, GROWTH_FACTOR, EXPONENT));
    }

    @Test
    void zeroGrowthOrExponentProducesAConstantPrice() {
        assertEquals(BASE_PRICE, PricingCalculator.priceForClaim(100, BASE_PRICE, 0.0D, EXPONENT));
        assertEquals(BASE_PRICE, PricingCalculator.priceForClaim(100, BASE_PRICE, GROWTH_FACTOR, 0.0D));
        assertEquals(12, PricingCalculator.totalPrice(50, 3, BASE_PRICE, 0.0D, EXPONENT));
    }

    @Test
    void overflowSaturatesInsteadOfWrapping() {
        assertEquals(
                Long.MAX_VALUE,
                PricingCalculator.priceForClaim(Long.MAX_VALUE, Long.MAX_VALUE - 1, 1_000_000.0D, 4.0D)
        );
        assertEquals(
                Long.MAX_VALUE,
                PricingCalculator.totalPrice(0, 2, Long.MAX_VALUE - 1, 0.0D, 0.0D)
        );
    }

    @Test
    void invalidInputsAreRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> PricingCalculator.priceForClaim(0, BASE_PRICE, GROWTH_FACTOR, EXPONENT)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> PricingCalculator.totalPrice(0, 0, BASE_PRICE, GROWTH_FACTOR, EXPONENT)
        );
    }
}

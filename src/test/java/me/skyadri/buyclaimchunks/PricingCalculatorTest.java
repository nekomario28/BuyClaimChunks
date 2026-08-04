package me.skyadri.buyclaimchunks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void batchPriceMatchesPublishedExamples() {
        assertEquals(31, PricingCalculator.totalPrice(0, 5, BASE_PRICE, GROWTH_FACTOR, EXPONENT));
        assertEquals(34, PricingCalculator.totalPrice(8, 3, BASE_PRICE, GROWTH_FACTOR, EXPONENT));
    }

    @Test
    void batchPriceSumsEveryClaimInTheRange() {
        long expected = PricingCalculator.priceForClaim(9, BASE_PRICE, GROWTH_FACTOR, EXPONENT)
                + PricingCalculator.priceForClaim(10, BASE_PRICE, GROWTH_FACTOR, EXPONENT)
                + PricingCalculator.priceForClaim(11, BASE_PRICE, GROWTH_FACTOR, EXPONENT);

        assertEquals(expected, PricingCalculator.totalPrice(8, 3, BASE_PRICE, GROWTH_FACTOR, EXPONENT));
    }

    @Test
    void cumulativePriceAndBudgetSearchAreInverseWithinTheConfiguredCap() {
        assertEquals(31, PricingCalculator.cumulativePrice(5, BASE_PRICE, GROWTH_FACTOR, EXPONENT));
        assertEquals(5, PricingCalculator.maxAffordableClaims(31, 100, BASE_PRICE, GROWTH_FACTOR, EXPONENT));
        assertEquals(4, PricingCalculator.maxAffordableClaims(30, 100, BASE_PRICE, GROWTH_FACTOR, EXPONENT));
    }

    @Test
    void increasedCurveCarriesTheShortfallIntoTheNextPurchase() {
        RepricedPurchase quote = PricingCalculator.quoteRepricedPurchase(
                2,
                2L,
                1,
                10,
                100,
                4L,
                0.0D,
                0.5D
        );

        assertFalse(quote.overflow());
        assertEquals(0, quote.compensationClaims());
        assertEquals(6L, quote.carriedDebt());
        assertEquals(10L, quote.paymentRequired());
        assertEquals(3, quote.resultingPaidClaims());
        assertEquals(12L, quote.resultingTotalSpent());
    }

    @Test
    void cheaperCurveGrantsClaimsSupportedByPreviousPayments() {
        RepricedPurchase quote = PricingCalculator.quoteRepricedPurchase(
                2,
                12L,
                1,
                10,
                100,
                4L,
                0.0D,
                0.5D
        );

        assertFalse(quote.overflow());
        assertEquals(1, quote.compensationClaims());
        assertEquals(2, quote.backendIncrease());
        assertEquals(4L, quote.paymentRequired());
        assertEquals(4, quote.resultingPaidClaims());
        assertEquals(16L, quote.resultingTotalSpent());
    }

    @Test
    void compensationIsLimitedByRemainingBackendCapacityAndCreditCarriesForward() {
        RepricedPurchase quote = PricingCalculator.quoteRepricedPurchase(
                2,
                20L,
                1,
                1,
                100,
                4L,
                0.0D,
                0.5D
        );

        assertEquals(1, quote.compensationClaims());
        assertEquals(2, quote.backendIncrease());
        assertEquals(0L, quote.paymentRequired());
        assertEquals(4, quote.resultingPaidClaims());
        assertEquals(20L, quote.resultingTotalSpent());
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
        assertThrows(
                IllegalArgumentException.class,
                () -> PricingCalculator.quoteRepricedPurchase(-1, 0, 1, 0, 100, BASE_PRICE, GROWTH_FACTOR, EXPONENT)
        );
    }
}

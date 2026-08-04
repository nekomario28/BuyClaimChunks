package me.skyadri.buyclaimchunks;

/**
 * Calculates claim prices without depending on Minecraft classes.
 */
public final class PricingCalculator {
    private PricingCalculator() {
    }

    /**
     * Calculates the price of a one-based personal extra-claim number.
     */
    public static long priceForClaim(long claimNumber, long basePrice, double growthFactor, double exponent) {
        if (claimNumber < 1 || basePrice < 1 || growthFactor < 0.0D || exponent < 0.0D) {
            throw new IllegalArgumentException("Pricing inputs are outside their supported range");
        }

        double calculated = basePrice + growthFactor * (Math.pow(claimNumber, exponent) - 1.0D);
        if (!Double.isFinite(calculated) || calculated >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }

        return Math.max(basePrice, Math.round(calculated));
    }

    /**
     * Sums the price of the next {@code purchaseAmount} claims.
     */
    public static long totalPrice(
            long currentClaims,
            int purchaseAmount,
            long basePrice,
            double growthFactor,
            double exponent
    ) {
        if (currentClaims < 0 || purchaseAmount < 1) {
            throw new IllegalArgumentException("Claim counts must be non-negative and purchase amount must be positive");
        }

        long total = 0;
        for (int offset = 1; offset <= purchaseAmount; offset++) {
            long claimNumber;
            try {
                claimNumber = Math.addExact(currentClaims, offset);
            } catch (ArithmeticException exception) {
                return Long.MAX_VALUE;
            }

            long price = priceForClaim(claimNumber, basePrice, growthFactor, exponent);
            if (price == Long.MAX_VALUE || Long.MAX_VALUE - total < price) {
                return Long.MAX_VALUE;
            }
            total += price;
        }
        return total;
    }

    /**
     * Calculates the cumulative price of all claims from one through
     * {@code claimCount}. Zero claims cost zero.
     */
    public static long cumulativePrice(
            int claimCount,
            long basePrice,
            double growthFactor,
            double exponent
    ) {
        if (claimCount < 0) {
            throw new IllegalArgumentException("Claim count must be non-negative");
        }
        if (claimCount == 0) {
            return 0L;
        }
        return totalPrice(0L, claimCount, basePrice, growthFactor, exponent);
    }

    /**
     * Finds the largest claim count whose cumulative price does not exceed the
     * supplied budget. The search is bounded by the configured maximum.
     */
    public static int maxAffordableClaims(
            long budget,
            int maximumClaims,
            long basePrice,
            double growthFactor,
            double exponent
    ) {
        if (budget < 0L || maximumClaims < 0) {
            throw new IllegalArgumentException("Budget and maximum claims must be non-negative");
        }

        int low = 0;
        int high = maximumClaims;
        while (low < high) {
            int middle = low + (high - low + 1) / 2;
            long cost = cumulativePrice(middle, basePrice, growthFactor, exponent);
            if (cost != Long.MAX_VALUE && cost <= budget) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        return low;
    }

    /**
     * Quotes a purchase against the player's lifetime payment ledger.
     *
     * <p>If the active curve became more expensive, {@code carriedDebt} is
     * included in the next payment. Existing claims are never removed. If the
     * curve became cheaper, already-spent currency can grant compensation
     * claims, bounded by {@code maximumCompensationClaims}.</p>
     */
    public static RepricedPurchase quoteRepricedPurchase(
            int paidClaims,
            long totalSpent,
            int purchaseAmount,
            int maximumCompensationClaims,
            int maximumClaims,
            long basePrice,
            double growthFactor,
            double exponent
    ) {
        if (paidClaims < 0 || totalSpent < 0L || purchaseAmount < 1
                || maximumCompensationClaims < 0 || maximumClaims < 0) {
            throw new IllegalArgumentException("Repricing inputs are outside their supported range");
        }

        long currentCurveCost = cumulativePrice(paidClaims, basePrice, growthFactor, exponent);
        if (currentCurveCost == Long.MAX_VALUE) {
            return RepricedPurchase.overflow();
        }
        long carriedDebt = Math.max(0L, currentCurveCost - totalSpent);

        int affordableClaims = maxAffordableClaims(
                totalSpent,
                maximumClaims,
                basePrice,
                growthFactor,
                exponent
        );
        int availableCompensation = Math.max(0, affordableClaims - paidClaims);
        int compensationClaims = Math.min(availableCompensation, maximumCompensationClaims);

        final int resultingPaidClaims;
        try {
            resultingPaidClaims = Math.addExact(
                    Math.addExact(paidClaims, compensationClaims),
                    purchaseAmount
            );
        } catch (ArithmeticException exception) {
            return RepricedPurchase.overflow();
        }

        long targetCurveCost = cumulativePrice(
                resultingPaidClaims,
                basePrice,
                growthFactor,
                exponent
        );
        if (targetCurveCost == Long.MAX_VALUE) {
            return RepricedPurchase.overflow();
        }

        long paymentRequired = Math.max(0L, targetCurveCost - totalSpent);
        final long resultingTotalSpent;
        try {
            resultingTotalSpent = Math.addExact(totalSpent, paymentRequired);
        } catch (ArithmeticException exception) {
            return RepricedPurchase.overflow();
        }

        return new RepricedPurchase(
                false,
                compensationClaims,
                purchaseAmount,
                resultingPaidClaims,
                paymentRequired,
                carriedDebt,
                resultingTotalSpent
        );
    }
}

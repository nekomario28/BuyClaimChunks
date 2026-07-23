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
}

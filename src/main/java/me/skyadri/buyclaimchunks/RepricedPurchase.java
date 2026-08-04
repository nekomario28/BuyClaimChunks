package me.skyadri.buyclaimchunks;

/**
 * Immutable result of repricing a purchase against the lifetime payment ledger.
 */
public record RepricedPurchase(
        boolean overflow,
        int compensationClaims,
        int purchasedClaims,
        int resultingPaidClaims,
        long paymentRequired,
        long carriedDebt,
        long resultingTotalSpent
) {
    public RepricedPurchase {
        if (!overflow && (compensationClaims < 0 || purchasedClaims < 1
                || resultingPaidClaims < 0 || paymentRequired < 0L
                || carriedDebt < 0L || resultingTotalSpent < 0L)) {
            throw new IllegalArgumentException("Repriced purchase values must be non-negative");
        }
    }

    public static RepricedPurchase overflowResult() {
        return new RepricedPurchase(true, 0, 1, 0, Long.MAX_VALUE, 0L, Long.MAX_VALUE);
    }

    public int backendIncrease() {
        if (overflow) {
            return 0;
        }
        return Math.addExact(compensationClaims, purchasedClaims);
    }
}

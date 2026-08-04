package me.skyadri.buyclaimchunks;

public record ClaimCapacityUpdate(Status status, int observedClaims, String detail) {
    public enum Status {
        SUCCESS,
        CONCURRENT_CHANGE,
        REJECTED,
        ERROR
    }

    public boolean success() {
        return status == Status.SUCCESS;
    }

    public static ClaimCapacityUpdate success(int observedClaims) {
        return new ClaimCapacityUpdate(Status.SUCCESS, observedClaims, "");
    }

    public static ClaimCapacityUpdate concurrentChange(int observedClaims) {
        return new ClaimCapacityUpdate(
                Status.CONCURRENT_CHANGE,
                observedClaims,
                "Claim capacity changed while the purchase was being prepared."
        );
    }

    public static ClaimCapacityUpdate rejected(int observedClaims, String detail) {
        return new ClaimCapacityUpdate(Status.REJECTED, observedClaims, detail);
    }

    public static ClaimCapacityUpdate error(int observedClaims, String detail) {
        return new ClaimCapacityUpdate(Status.ERROR, observedClaims, detail);
    }
}

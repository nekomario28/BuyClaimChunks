package me.skyadri.buyclaimchunks;

public final class ClaimCapacityBackends {
    private ClaimCapacityBackends() {
    }

    public static ClaimCapacityBackend create() {
        return new OpenPacClaimCapacityBackend();
    }
}

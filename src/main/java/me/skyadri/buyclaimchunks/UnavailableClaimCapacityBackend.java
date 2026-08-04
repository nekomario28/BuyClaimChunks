package me.skyadri.buyclaimchunks;

import net.minecraft.server.level.ServerPlayer;

final class UnavailableClaimCapacityBackend implements ClaimCapacityBackend {
    private final String reason;

    UnavailableClaimCapacityBackend(String reason) {
        this.reason = reason;
    }

    @Override
    public String id() {
        return "unavailable";
    }

    @Override
    public int getExtraClaims(ServerPlayer player) {
        throw new IllegalStateException(reason);
    }

    @Override
    public int getFullClaimLimit(ServerPlayer player) {
        throw new IllegalStateException(reason);
    }

    @Override
    public ClaimCapacityUpdate setExtraClaims(ServerPlayer player, int expectedCurrent, int newValue) {
        return ClaimCapacityUpdate.error(expectedCurrent, reason);
    }
}

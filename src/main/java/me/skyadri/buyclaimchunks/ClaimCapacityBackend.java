package me.skyadri.buyclaimchunks;

import net.minecraft.server.level.ServerPlayer;

/**
 * Backend-neutral access to the personal extra claim capacity owned by the
 * selected claim mod.
 */
public interface ClaimCapacityBackend {
    String id();

    int getExtraClaims(ServerPlayer player);

    int getFullClaimLimit(ServerPlayer player);

    /**
     * Sets the backend-owned extra capacity only when it still equals the
     * expected value. Implementations must re-read and verify the final value.
     */
    ClaimCapacityUpdate setExtraClaims(ServerPlayer player, int expectedCurrent, int newValue);
}

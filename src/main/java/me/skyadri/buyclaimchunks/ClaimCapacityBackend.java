package me.skyadri.buyclaimchunks;

import net.minecraft.server.level.ServerPlayer;

/**
 * Backend-neutral access to the extra claim capacity owned by the player that
 * executes /buyclaim.
 */
public interface ClaimCapacityBackend {
    String id();

    int getExtraClaims(ServerPlayer player);

    int getFullClaimLimit(ServerPlayer player);

    /**
     * Describes how the backend currently uses this player's quota. The
     * purchase target itself remains the player's UUID; this method must not
     * redirect a purchase to another player or party.
     */
    default ClaimCapacityContext getCapacityContext(ServerPlayer player) {
        return ClaimCapacityContext.personal(player.getUUID());
    }

    /**
     * Sets the backend-owned extra capacity only when it still equals the
     * expected value. Implementations must re-read and verify the final value.
     */
    ClaimCapacityUpdate setExtraClaims(ServerPlayer player, int expectedCurrent, int newValue);
}

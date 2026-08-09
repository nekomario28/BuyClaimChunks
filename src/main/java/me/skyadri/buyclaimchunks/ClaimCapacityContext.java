package me.skyadri.buyclaimchunks;

import java.util.UUID;

/**
 * Describes how the selected backend will use the capacity owned by the
 * player that executes /buyclaim. This is informational only: BuyClaimChunks
 * never transfers a purchase ledger or bonus quota between players or parties.
 */
public record ClaimCapacityContext(
        Kind kind,
        UUID playerId,
        UUID partyId,
        UUID partyOwnerId,
        String partyOwnerName
) {
    public enum Kind {
        /** The player's own capacity is used only as their player pool. */
        PERSONAL,
        /** OpenPAC party-owned claims use this owner UUID as their shared pool. */
        PARTY_OWNER_SHARED,
        /** The player is a party member, but purchases still affect only their own player UUID. */
        PARTY_MEMBER_PERSONAL,
        /** The backend could not determine the context safely. */
        UNKNOWN
    }

    public static ClaimCapacityContext personal(UUID playerId) {
        return new ClaimCapacityContext(Kind.PERSONAL, playerId, null, null, null);
    }

    public static ClaimCapacityContext ownerShared(
            UUID playerId,
            UUID partyId,
            String ownerName
    ) {
        return new ClaimCapacityContext(
                Kind.PARTY_OWNER_SHARED,
                playerId,
                partyId,
                playerId,
                ownerName
        );
    }

    public static ClaimCapacityContext memberPersonal(
            UUID playerId,
            UUID partyId,
            UUID ownerId,
            String ownerName
    ) {
        return new ClaimCapacityContext(
                Kind.PARTY_MEMBER_PERSONAL,
                playerId,
                partyId,
                ownerId,
                ownerName
        );
    }

    public static ClaimCapacityContext unknown(UUID playerId) {
        return new ClaimCapacityContext(Kind.UNKNOWN, playerId, null, null, null);
    }
}

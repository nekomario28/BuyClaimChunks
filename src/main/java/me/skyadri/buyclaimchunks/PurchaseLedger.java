package me.skyadri.buyclaimchunks;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Server-authoritative payment history used to reprice future claim purchases
 * after an administrator changes the active price curve.
 */
public final class PurchaseLedger extends SavedData {
    private static final String DATA_NAME = "buyclaimchunks_purchase_ledger";
    private static final int SCHEMA_VERSION = 1;
    private static final SavedData.Factory<PurchaseLedger> FACTORY =
            new SavedData.Factory<>(PurchaseLedger::new, PurchaseLedger::load);

    private final Map<UUID, MutableAccount> accounts = new HashMap<>();

    public static PurchaseLedger get(ServerPlayer player) {
        MinecraftServer server = Objects.requireNonNull(player.getServer(), "Player is not attached to a server");
        ServerLevel overworld = Objects.requireNonNull(
                server.getLevel(Level.OVERWORLD),
                "Server has no overworld for purchase ledger storage"
        );
        return overworld.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public static PurchaseLedger load(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        PurchaseLedger ledger = new PurchaseLedger();
        ListTag players = tag.getList("Players", Tag.TAG_COMPOUND);
        for (int index = 0; index < players.size(); index++) {
            CompoundTag playerTag = players.getCompound(index);
            if (!playerTag.hasUUID("Player")) {
                continue;
            }

            UUID playerId = playerTag.getUUID("Player");
            String currencyItemId = playerTag.getString("CurrencyItem");
            int paidClaims = Math.max(0, playerTag.getInt("PaidClaims"));
            long totalSpent = Math.max(0L, playerTag.getLong("TotalSpent"));
            if (currencyItemId.isBlank()) {
                continue;
            }

            ledger.accounts.put(
                    playerId,
                    new MutableAccount(currencyItemId, paidClaims, totalSpent)
            );
        }
        return ledger;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        tag.putInt("SchemaVersion", SCHEMA_VERSION);
        ListTag players = new ListTag();
        for (Map.Entry<UUID, MutableAccount> mapEntry : accounts.entrySet()) {
            MutableAccount account = mapEntry.getValue();
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID("Player", mapEntry.getKey());
            playerTag.putString("CurrencyItem", account.currencyItemId);
            playerTag.putInt("PaidClaims", account.paidClaims);
            playerTag.putLong("TotalSpent", account.totalSpent);
            players.add(playerTag);
        }
        tag.put("Players", players);
        return tag;
    }

    /**
     * Returns the active account, creating a compatibility baseline when this
     * world predates the payment ledger.
     *
     * <p>Legacy capacity cannot be separated from administrator grants, so the
     * first baseline treats the currently reported backend capacity as already
     * paid at the active curve. This preserves the pre-ledger next price and
     * starts exact tracking from that point onward.</p>
     *
     * <p>A currency item change cannot be valued automatically. It starts a new
     * baseline at the same paid-claim count under the active curve, while curve
     * changes using the same currency retain the true cumulative spent value.</p>
     */
    public Account getOrCreateAccount(
            UUID playerId,
            String currencyItemId,
            int currentBackendClaims,
            long basePrice,
            double growthFactor,
            double exponent
    ) {
        if (currentBackendClaims < 0 || currencyItemId == null || currencyItemId.isBlank()) {
            throw new IllegalArgumentException("Invalid purchase-ledger baseline");
        }

        MutableAccount mutable = accounts.get(playerId);
        if (mutable == null) {
            long baselineSpent = requiredBaselineCost(
                    currentBackendClaims,
                    basePrice,
                    growthFactor,
                    exponent
            );
            mutable = new MutableAccount(currencyItemId, currentBackendClaims, baselineSpent);
            accounts.put(playerId, mutable);
            setDirty();
        } else if (!mutable.currencyItemId.equals(currencyItemId)) {
            long baselineSpent = requiredBaselineCost(
                    mutable.paidClaims,
                    basePrice,
                    growthFactor,
                    exponent
            );
            mutable.currencyItemId = currencyItemId;
            mutable.totalSpent = baselineSpent;
            setDirty();
        }

        return mutable.snapshot();
    }

    /**
     * Commits a completed transaction only if the account still matches the
     * quote that was used. Commands run on the server thread, but this check
     * also protects future callers from stale ledger writes.
     */
    public boolean commit(
            UUID playerId,
            Account expected,
            int resultingPaidClaims,
            long resultingTotalSpent
    ) {
        MutableAccount mutable = accounts.get(playerId);
        if (mutable == null || !mutable.snapshot().equals(expected)) {
            return false;
        }
        if (resultingPaidClaims < expected.paidClaims || resultingTotalSpent < expected.totalSpent) {
            throw new IllegalArgumentException("Purchase ledger values cannot move backwards");
        }

        mutable.paidClaims = resultingPaidClaims;
        mutable.totalSpent = resultingTotalSpent;
        setDirty();
        return true;
    }

    Account getAccountForTests(UUID playerId) {
        MutableAccount mutable = accounts.get(playerId);
        return mutable == null ? null : mutable.snapshot();
    }

    private static long requiredBaselineCost(
            int claims,
            long basePrice,
            double growthFactor,
            double exponent
    ) {
        long cost = PricingCalculator.cumulativePrice(claims, basePrice, growthFactor, exponent);
        if (cost == Long.MAX_VALUE) {
            throw new IllegalStateException("The active price curve is too large to initialize a purchase ledger");
        }
        return cost;
    }

    public record Account(String currencyItemId, int paidClaims, long totalSpent) {
        public Account {
            if (currencyItemId == null || currencyItemId.isBlank() || paidClaims < 0 || totalSpent < 0L) {
                throw new IllegalArgumentException("Invalid purchase ledger account");
            }
        }
    }

    private static final class MutableAccount {
        private String currencyItemId;
        private int paidClaims;
        private long totalSpent;

        private MutableAccount(String currencyItemId, int paidClaims, long totalSpent) {
            this.currencyItemId = currencyItemId;
            this.paidClaims = paidClaims;
            this.totalSpent = totalSpent;
        }

        private Account snapshot() {
            return new Account(currencyItemId, paidClaims, totalSpent);
        }
    }
}

package me.skyadri.buyclaimchunks;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftbchunks.api.ChunkTeamData;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import net.minecraft.server.level.ServerPlayer;

final class FtbClaimCapacityBackend implements ClaimCapacityBackend {
    @Override
    public String id() {
        return "ftb";
    }

    @Override
    public int getExtraClaims(ServerPlayer player) {
        if (player == null) {
            return 0;
        }

        ChunkTeamData data = FTBChunksAPI.api().getManager().getPersonalData(player.getUUID());
        return data == null ? 0 : data.getExtraClaimChunks();
    }

    @Override
    public int getFullClaimLimit(ServerPlayer player) {
        // The purchase contract and pricing source of truth for the FTB build is
        // the personal extra-claim value, matching all releases through 1.1.1.
        return getExtraClaims(player);
    }

    @Override
    public ClaimCapacityUpdate setExtraClaims(ServerPlayer player, int expectedCurrent, int newValue) {
        int observedBefore = getExtraClaims(player);
        if (observedBefore != expectedCurrent) {
            return ClaimCapacityUpdate.concurrentChange(observedBefore);
        }

        String command = "ftbchunks admin extra_claim_chunks "
                + player.getGameProfile().getName()
                + " set " + newValue;

        try {
            var server = player.getServer();
            if (server == null) {
                return ClaimCapacityUpdate.error(observedBefore, "Minecraft server is unavailable.");
            }

            server.getCommands().getDispatcher().execute(
                    command,
                    server.createCommandSourceStack()
                            .withPermission(4)
                            .withSuppressedOutput()
            );
        } catch (CommandSyntaxException exception) {
            BuyClaimChunks.LOGGER.warn(
                    "FTB Chunks rejected an extra-claim update for player {}",
                    player.getGameProfile().getName(),
                    exception
            );
            return ClaimCapacityUpdate.rejected(observedBefore, "FTB Chunks rejected the capacity update.");
        } catch (RuntimeException exception) {
            BuyClaimChunks.LOGGER.error(
                    "FTB Chunks capacity update failed for player {}",
                    player.getGameProfile().getName(),
                    exception
            );
            return ClaimCapacityUpdate.error(observedBefore, "FTB Chunks capacity update failed unexpectedly.");
        }

        int observedAfter = getExtraClaims(player);
        if (observedAfter != newValue) {
            return ClaimCapacityUpdate.rejected(
                    observedAfter,
                    "FTB Chunks did not persist the requested capacity value."
            );
        }
        return ClaimCapacityUpdate.success(observedAfter);
    }
}

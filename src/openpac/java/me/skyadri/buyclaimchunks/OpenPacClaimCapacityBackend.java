package me.skyadri.buyclaimchunks;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.player.config.api.v2.IPlayerConfigAPI;
import xaero.pac.common.server.player.config.api.v2.PlayerConfigOptions;

final class OpenPacClaimCapacityBackend implements ClaimCapacityBackend {
    @Override
    public String id() {
        return "openpac";
    }

    @Override
    public int getExtraClaims(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return 0;
        }

        return OpenPACServerAPI.get(server)
                .getPlayerConfigManager()
                .getLoadedConfig(player.getUUID())
                .getEffective(PlayerConfigOptions.BONUS_CHUNK_CLAIMS);
    }

    @Override
    public int getFullClaimLimit(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return 0;
        }
        return OpenPACServerAPI.get(server)
                .getServerClaimsManager()
                .getPlayerFullClaimLimit(player);
    }

    @Override
    public ClaimCapacityUpdate setExtraClaims(ServerPlayer player, int expectedCurrent, int newValue) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return ClaimCapacityUpdate.error(expectedCurrent, "Minecraft server is unavailable.");
        }

        try {
            OpenPACServerAPI api = OpenPACServerAPI.get(server);
            IPlayerConfigAPI playerConfig = api
                    .getPlayerConfigManager()
                    .getLoadedConfig(player.getUUID());

            if (!playerConfig.isOptionAllowed(PlayerConfigOptions.BONUS_CHUNK_CLAIMS)) {
                return ClaimCapacityUpdate.rejected(
                        getExtraClaims(player),
                        "OpenPAC does not allow bonus claim capacity in this player config."
                );
            }

            int observedBefore = playerConfig.getEffective(PlayerConfigOptions.BONUS_CHUNK_CLAIMS);
            if (observedBefore != expectedCurrent) {
                return ClaimCapacityUpdate.concurrentChange(observedBefore);
            }

            int baseBefore = api.getServerClaimsManager().getPlayerBaseClaimLimit(player);
            int fullBefore = api.getServerClaimsManager().getPlayerFullClaimLimit(player);
            if (fullBefore != baseBefore + observedBefore) {
                BuyClaimChunks.LOGGER.warn(
                        "OpenPAC claim limit invariant is unexpected for player {}: base={}, bonus={}, full={}",
                        player.getGameProfile().getName(),
                        baseBefore,
                        observedBefore,
                        fullBefore
                );
            }

            IPlayerConfigAPI.SetResult result = playerConfig.tryToSet(
                    PlayerConfigOptions.BONUS_CHUNK_CLAIMS,
                    newValue
            );
            if (result != IPlayerConfigAPI.SetResult.SUCCESS
                    && result != IPlayerConfigAPI.SetResult.DEFAULTED) {
                return ClaimCapacityUpdate.rejected(
                        observedBefore,
                        "OpenPAC rejected the bonus claim update: " + result
                );
            }

            int observedAfter = playerConfig.getEffective(PlayerConfigOptions.BONUS_CHUNK_CLAIMS);
            int baseAfter = api.getServerClaimsManager().getPlayerBaseClaimLimit(player);
            int fullAfter = api.getServerClaimsManager().getPlayerFullClaimLimit(player);

            if (observedAfter != newValue) {
                return ClaimCapacityUpdate.rejected(
                        observedAfter,
                        "OpenPAC did not persist the requested bonus claim value."
                );
            }
            if (fullAfter != baseAfter + observedAfter) {
                return ClaimCapacityUpdate.rejected(
                        observedAfter,
                        "OpenPAC full claim limit does not equal base plus bonus capacity."
                );
            }

            if (baseAfter != 0) {
                BuyClaimChunks.LOGGER.warn(
                        "OpenPAC base claim limit for player {} is {}. The all-paid claim model requires a base limit of 0.",
                        player.getGameProfile().getName(),
                        baseAfter
                );
            }

            return ClaimCapacityUpdate.success(observedAfter);
        } catch (RuntimeException exception) {
            BuyClaimChunks.LOGGER.error(
                    "OpenPAC capacity update failed for player {}",
                    player.getGameProfile().getName(),
                    exception
            );
            return ClaimCapacityUpdate.error(expectedCurrent, "OpenPAC capacity update failed unexpectedly.");
        }
    }
}

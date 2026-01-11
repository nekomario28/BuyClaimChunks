package me.skyadri.buyclaimchunks;

import dev.ftb.mods.ftbchunks.api.ClaimedChunkManager;
import dev.ftb.mods.ftbchunks.api.ChunkTeamData;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import net.minecraft.server.level.ServerPlayer;

public class ClaimHelper {

    public static int getExtraClaims(ServerPlayer player) {
        if (player == null) return 0; // safety check

        ClaimedChunkManager manager = FTBChunksAPI.api().getManager();
        ChunkTeamData data = manager.getOrCreateData(player);

        return data.getExtraClaimChunks();
    }

}

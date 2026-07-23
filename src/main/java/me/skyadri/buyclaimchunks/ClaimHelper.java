package me.skyadri.buyclaimchunks;

import dev.ftb.mods.ftbchunks.api.ClaimedChunkManager;
import dev.ftb.mods.ftbchunks.api.ChunkTeamData;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import net.minecraft.server.level.ServerPlayer;

public class ClaimHelper {

    public static int getExtraClaims(ServerPlayer player) {
        if (player == null) return 0; // safety check

        ClaimedChunkManager manager = FTBChunksAPI.api().getManager();
        ChunkTeamData data = manager.getPersonalData(player.getUUID());

        return data == null ? 0 : data.getExtraClaimChunks();
    }

}

package me.skyadri.buyclaimchunks;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

@Mod(BuyClaimChunks.MOD_ID)
public class BuyClaimChunks {
    public static final String MOD_ID = "buyclaimchunks";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final ClaimCapacityBackend CLAIM_BACKEND = ClaimCapacityBackends.create();

    public BuyClaimChunks(ModContainer modContainer) {
        NeoForge.EVENT_BUS.register(this);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        LOGGER.info("BuyClaimChunks Continued initialized with {} backend", CLAIM_BACKEND.id());
    }

    public static ClaimCapacityBackend getClaimBackend() {
        return CLAIM_BACKEND;
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        BuyClaimCommand.register(dispatcher);
    }
}

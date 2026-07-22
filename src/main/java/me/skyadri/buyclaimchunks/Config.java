package me.skyadri.buyclaimchunks;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {

    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    // ---------------- CONFIG OPTIONS ----------------
    // Item required to buy a claim chunk
    public static final ModConfigSpec.ConfigValue<String> ITEM_REQUIRED;

    // Amount of the item required
    public static final ModConfigSpec.IntValue AMOUNT_REQUIRED;

    // Maximum number of extra claim chunks a player can buy
    public static final ModConfigSpec.IntValue MAX_EXTRA_CLAIMS;

    static {
        BUILDER.comment("BuyClaimChunks Mod Configuration").push("general");

        ITEM_REQUIRED = BUILDER
                .comment("The item required to buy an extra claim chunk (example: minecraft:diamond)")
                .define("itemRequired", "minecraft:diamond");

        AMOUNT_REQUIRED = BUILDER
                .comment("The amount of the required item needed per claim chunk purchase")
                .defineInRange("amountRequired", 1, 1, Integer.MAX_VALUE);

        MAX_EXTRA_CLAIMS = BUILDER
                .comment("Maximum number of extra claim chunks a player can have")
                .defineInRange("maxExtraClaims", 100, 1, Integer.MAX_VALUE);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    // ---------------- HELPER METHODS ----------------
    /** Returns the configured item as a ResourceLocation */
    public static ResourceLocation getItemRequired() {
        return ResourceLocation.tryParse(ITEM_REQUIRED.get());
    }

    /** Returns the configured amount of the item required */
    public static int getAmountRequired() {
        return AMOUNT_REQUIRED.get();
    }

    /** Returns the configured maximum number of extra claims per player */
    public static int getMaxExtraClaims() {
        return MAX_EXTRA_CLAIMS.get();
    }
}

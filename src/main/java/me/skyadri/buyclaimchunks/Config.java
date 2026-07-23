package me.skyadri.buyclaimchunks;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {

    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    // ---------------- CONFIG OPTIONS ----------------
    // Item required to buy a claim chunk
    public static final ModConfigSpec.ConfigValue<String> ITEM_REQUIRED;

    // Price of the first extra claim
    public static final ModConfigSpec.IntValue AMOUNT_REQUIRED;

    // Controls how quickly the price increases
    public static final ModConfigSpec.DoubleValue PRICE_GROWTH_FACTOR;

    // Controls the shape of the price curve
    public static final ModConfigSpec.DoubleValue PRICE_EXPONENT;

    // Maximum number of extra claim chunks a player can buy
    public static final ModConfigSpec.IntValue MAX_EXTRA_CLAIMS;

    // Maximum number of claims that can be bought with one command
    public static final ModConfigSpec.IntValue MAX_PURCHASE_AMOUNT;

    static {
        BUILDER.comment("BuyClaimChunks Mod Configuration").push("general");

        ITEM_REQUIRED = BUILDER
                .comment("The item required to buy an extra claim chunk (example: minecraft:diamond)")
                .define("itemRequired", "minecraft:diamond");

        AMOUNT_REQUIRED = BUILDER
                .comment(
                        "Base price: the number of items required when the personal extra-claim number is 1.",
                        "Existing config files keep their previous value; set this to 4 for the new default curve."
                )
                .defineInRange("amountRequired", 4, 1, Integer.MAX_VALUE);

        PRICE_GROWTH_FACTOR = BUILDER
                .comment(
                        "Growth factor in: round(amountRequired + priceGrowthFactor * (claimNumber^priceExponent - 1)).",
                        "Set this to 0 for a constant price."
                )
                .defineInRange("priceGrowthFactor", 3.45D, 0.0D, 1_000_000.0D);

        PRICE_EXPONENT = BUILDER
                .comment(
                        "Exponent in the progressive price formula.",
                        "The default 0.5 produces a square-root curve. Set this to 0 for a constant price."
                )
                .defineInRange("priceExponent", 0.5D, 0.0D, 4.0D);

        MAX_EXTRA_CLAIMS = BUILDER
                .comment("Maximum number of extra claim chunks a player can have")
                .defineInRange("maxExtraClaims", 100, 1, Integer.MAX_VALUE);

        MAX_PURCHASE_AMOUNT = BUILDER
                .comment("Maximum number of extra claims that can be purchased with one /buyclaim command")
                .defineInRange("maxPurchaseAmount", 100, 1, 10_000);

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

    /** Returns the configured price growth factor */
    public static double getPriceGrowthFactor() {
        return PRICE_GROWTH_FACTOR.get();
    }

    /** Returns the configured price exponent */
    public static double getPriceExponent() {
        return PRICE_EXPONENT.get();
    }

    /** Returns the configured maximum number of extra claims per player */
    public static int getMaxExtraClaims() {
        return MAX_EXTRA_CLAIMS.get();
    }

    /** Returns the maximum number of claims allowed in one purchase */
    public static int getMaxPurchaseAmount() {
        return MAX_PURCHASE_AMOUNT.get();
    }
}

package me.skyadri.buyclaimchunks;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class BuyClaimCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("buyclaim")
                        .then(
                                Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(context -> executeBuy(context.getSource(), IntegerArgumentType.getInteger(context, "amount")))
                        )
                        .executes(context -> executeBuy(context.getSource(), 1))
        );
    }

    private static int executeBuy(CommandSourceStack source, int amount) {
        ServerPlayer player;

        try {
            player = source.getPlayerOrException();
        } catch (CommandSyntaxException exception) {
            source.sendFailure(
                    Component.literal("This command can only be run by a player!").withStyle(ChatFormatting.RED)
            );
            return 0;
        }

        int maxPurchaseAmount = Config.getMaxPurchaseAmount();
        if (amount > maxPurchaseAmount) {
            player.sendSystemMessage(
                    Component.literal("You can buy at most ")
                            .append(Component.literal(String.valueOf(maxPurchaseAmount)).withStyle(ChatFormatting.LIGHT_PURPLE))
                            .append(Component.literal(" extra claim chunks at once!"))
                            .withStyle(ChatFormatting.RED)
            );
            return 0;
        }

        int maxClaims = Config.getMaxExtraClaims();
        int currentClaims = ClaimHelper.getExtraClaims(player);

        if ((long) currentClaims + amount > maxClaims) {
            player.sendSystemMessage(
                    Component.literal("You cannot buy that many extra claim chunks! Maximum of ")
                            .append(Component.literal(String.valueOf(maxClaims)).withStyle(ChatFormatting.LIGHT_PURPLE))
                            .append(Component.literal(" reached!"))
                            .withStyle(ChatFormatting.RED)
            );
            return 0;
        }

        ResourceLocation itemId = Config.getItemRequired();
        long totalRequired = PricingCalculator.totalPrice(
                currentClaims,
                amount,
                Config.getAmountRequired(),
                Config.getPriceGrowthFactor(),
                Config.getPriceExponent()
        );

        if (totalRequired == Long.MAX_VALUE) {
            player.sendSystemMessage(
                    Component.literal("The calculated purchase cost is too large. Check the server configuration.")
                            .withStyle(ChatFormatting.RED)
            );
            return 0;
        }

        Item requiredItem = itemId == null
                ? null
                : BuiltInRegistries.ITEM.getOptional(itemId).orElse(null);
        if (requiredItem == null) {
            player.sendSystemMessage(
                    Component.literal("The configured item for buying claim chunks does not exist!").withStyle(ChatFormatting.RED)
            );
            return 0;
        }

        long playerAmount = InventoryPayment.count(player.getInventory(), requiredItem);
        if (playerAmount < totalRequired) {
            String itemDisplay = requiredItem.getName(new ItemStack(requiredItem)).getString();
            String chunkText = amount == 1 ? "claim chunk" : "claim chunks";

            player.sendSystemMessage(
                    Component.literal("You need ")
                            .append(Component.literal(String.valueOf(totalRequired)).withStyle(ChatFormatting.LIGHT_PURPLE))
                            .append(Component.literal(" "))
                            .append(Component.literal(itemDisplay).withStyle(ChatFormatting.LIGHT_PURPLE))
                            .append(Component.literal(" to buy " + amount + " " + chunkText + "!"))
                            .withStyle(ChatFormatting.RED)
            );
            return 0;
        }

        String ftbCmd = "ftbchunks admin extra_claim_chunks "
                + player.getName().getString()
                + " add " + amount;

        var server = source.getServer();
        int commandResult;
        try {
            commandResult = server.getCommands().getDispatcher().execute(
                    ftbCmd,
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
            commandResult = 0;
        }

        if (commandResult <= 0) {
            player.sendSystemMessage(
                    Component.literal("The claim purchase failed. No items were consumed.")
                            .withStyle(ChatFormatting.RED)
            );
            return 0;
        }

        if (!InventoryPayment.consume(player.getInventory(), requiredItem, totalRequired)) {
            BuyClaimChunks.LOGGER.error(
                    "FTB Chunks increased claim capacity for player {}, but the validated payment could not be consumed",
                    player.getGameProfile().getName()
            );
            player.sendSystemMessage(
                    Component.literal("Claim capacity was increased, but payment could not be completed. Contact a server administrator.")
                            .withStyle(ChatFormatting.RED)
            );
            return 0;
        }

        player.sendSystemMessage(
                Component.literal("You have successfully bought " + amount + " extra claim chunk(s)!")
                        .withStyle(ChatFormatting.GREEN)
        );

        return 1;
    }
}

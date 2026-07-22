package me.skyadri.buyclaimchunks;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.concurrent.atomic.AtomicInteger;

public class BuyClaimCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("buyclaim")
                        .then(
                                Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(context -> executeBuy(context.getSource(), IntegerArgumentType.getInteger(context, "amount")))
                        )
                        .executes(context -> executeBuy(context.getSource(), 1)) // default 1
        );
    }

    private static int executeBuy(CommandSourceStack source, int amount) {
        ServerPlayer player;

        // Ensure command is run by a player
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(
                    Component.literal("This command can only be run by a player!").withStyle(ChatFormatting.RED)
            );
            return 0;
        }

        // Config and max claims
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

        // Item cost
        ResourceLocation itemId = Config.getItemRequired();
        int requiredPerChunk = Config.getAmountRequired();
        long totalRequired = (long) requiredPerChunk * amount;

        Item requiredItem = itemId == null
                ? null
                : BuiltInRegistries.ITEM.getOptional(itemId).orElse(null);
        if (requiredItem == null) {
            player.sendSystemMessage(
                    Component.literal("The configured item for buying claim chunks does not exist!").withStyle(ChatFormatting.RED)
            );
            return 0;
        }

        // Count items in inventory
        long playerAmount = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == requiredItem) playerAmount += stack.getCount();
        }

        if (playerAmount < totalRequired) {
            String itemDisplay = requiredItem.getName(new ItemStack(requiredItem)).getString();
            String chunkText = amount == 1 ? "claim chunk" : "claim chunks"; // singular/plural

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


        // Execute FTB Chunks command before charging the player. If the command
        // fails (for example after an FTB Chunks command change), no items are lost.
        String ftbCmd = "ftbchunks admin extra_claim_chunks "
                + player.getName().getString()
                + " add " + amount;

        var server = source.getServer();
        AtomicInteger commandResult = new AtomicInteger();
        server.getCommands().performPrefixedCommand(
                server.createCommandSourceStack()
                        .withPermission(4)
                        .withSuppressedOutput()
                        .withCallback((success, result) -> commandResult.set(success ? result : 0)),
                ftbCmd
        );

        if (commandResult.get() <= 0) {
            player.sendSystemMessage(
                    Component.literal("The claim purchase failed. No items were consumed.")
                            .withStyle(ChatFormatting.RED)
            );
            return 0;
        }

        // Payment is consumed only after FTB Chunks confirms the update.
        long remaining = totalRequired;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == requiredItem) {
                int remove = (int) Math.min(stack.getCount(), remaining);
                stack.shrink(remove);
                remaining -= remove;
                if (remaining <= 0) break;
            }
        }

        // Success message
        player.sendSystemMessage(
                Component.literal("You have successfully bought " + amount + " extra claim chunk(s)!")
                        .withStyle(ChatFormatting.GREEN)
        );

        return 1;
    }
}

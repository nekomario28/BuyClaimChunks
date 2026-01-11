package me.skyadri.buyclaimchunks;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

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

        if (currentClaims + amount > maxClaims) {
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
        int totalRequired = requiredPerChunk * amount;

        Item requiredItem = ForgeRegistries.ITEMS.getValue(itemId);
        if (requiredItem == null) {
            player.sendSystemMessage(
                    Component.literal("The configured item for buying claim chunks does not exist!").withStyle(ChatFormatting.RED)
            );
            return 0;
        }

        // Count items in inventory
        int playerAmount = 0;
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


        // Remove items
        int remaining = totalRequired;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == requiredItem) {
                int remove = Math.min(stack.getCount(), remaining);
                stack.shrink(remove);
                remaining -= remove;
                if (remaining <= 0) break;
            }
        }

        // Execute FTB Chunks command
        String ftbCmd = "ftbchunks admin extra_claim_chunks "
                + player.getName().getString()
                + " add " + amount;

        var server = source.getServer();
        server.getCommands().performPrefixedCommand(
                server.createCommandSourceStack()
                        .withPermission(4)
                        .withSuppressedOutput(),
                ftbCmd
        );

        // Success message
        player.sendSystemMessage(
                Component.literal("You have successfully bought " + amount + " extra claim chunk(s)!")
                        .withStyle(ChatFormatting.GREEN)
        );

        return 1;
    }
}

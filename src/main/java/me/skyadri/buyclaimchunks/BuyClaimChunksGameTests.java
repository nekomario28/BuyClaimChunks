package me.skyadri.buyclaimchunks;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(BuyClaimChunks.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BuyClaimChunksGameTests {
    private BuyClaimChunksGameTests() {
    }

    @GameTest(template = "empty")
    public static void buyClaimCommandIsRegistered(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        boolean registered = level.getServer()
                .getCommands()
                .getDispatcher()
                .getRoot()
                .getChild("buyclaim") != null;

        helper.assertTrue(registered, "Expected /buyclaim to be registered");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void paymentCanSpanMultipleInventoryStacks(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().clearContent();
        player.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 20));
        player.getInventory().setItem(1, new ItemStack(Items.DIAMOND, 30));

        helper.assertValueEqual(
                InventoryPayment.count(player.getInventory(), Items.DIAMOND),
                50L,
                "diamond count before payment"
        );
        helper.assertTrue(
                InventoryPayment.consume(player.getInventory(), Items.DIAMOND, 35L),
                "Expected payment to consume across multiple stacks"
        );
        helper.assertValueEqual(
                InventoryPayment.count(player.getInventory(), Items.DIAMOND),
                15L,
                "diamond count after payment"
        );
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void insufficientPaymentDoesNotConsumeItems(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().clearContent();
        player.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 10));

        helper.assertFalse(
                InventoryPayment.consume(player.getInventory(), Items.DIAMOND, 11L),
                "Expected an insufficient payment to be rejected"
        );
        helper.assertValueEqual(
                InventoryPayment.count(player.getInventory(), Items.DIAMOND),
                10L,
                "diamond count after rejected payment"
        );
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void defaultPricingIsAvailableInGame(GameTestHelper helper) {
        long total = PricingCalculator.totalPrice(0, 5, 4, 3.45D, 0.5D);
        helper.assertValueEqual(total, 31L, "first five default claim prices");
        helper.succeed();
    }
}

package me.skyadri.buyclaimchunks;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(BuyClaimChunks.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BuyClaimChunksGameTests {
    private BuyClaimChunksGameTests() {
    }

    @GameTest(template = "empty")
    public static void defaultPricingIsAvailableInGame(GameTestHelper helper) {
        long total = PricingCalculator.totalPrice(0, 5, 4, 3.45D, 0.5D);
        if (total != 31L) {
            helper.fail("Expected the first five default claim slots to cost 31 items, got " + total);
            return;
        }

        helper.succeed();
    }
}

package me.skyadri.buyclaimchunks;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@GameTestHolder(BuyClaimChunks.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BuyClaimChunksRepricingGameTests {
    private static final String EXPECTED_BACKEND_PROPERTY = "buyclaimchunks.expectedBackend";

    private BuyClaimChunksRepricingGameTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void carriedDebtIsCollectedByTheNextRealPurchase(GameTestHelper helper) {
        if (!hasRealBackend()) {
            helper.succeed();
            return;
        }

        ServerPlayer player = makeConnectedPlayer(helper, "bclaim-debt");
        helper.runAfterDelay(5, () -> {
            try {
                ClaimCapacityBackend backend = BuyClaimChunks.getClaimBackend();
                resetBackendClaims(helper, backend, player, 1);

                PurchaseLedger ledger = PurchaseLedger.get(player);
                PurchaseLedger.Account baseline = ledger.getOrCreateAccount(
                        player.getUUID(),
                        "minecraft:diamond",
                        1,
                        4L,
                        3.45D,
                        0.5D
                );
                PurchaseLedger.Account underpaid = new PurchaseLedger.Account("minecraft:diamond", 1, 1L);
                helper.assertTrue(
                        ledger.compareAndSet(player.getUUID(), baseline, underpaid),
                        "debt test ledger seed must succeed"
                );

                player.getInventory().clearContent();
                player.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 8));

                int result = helper.getLevel().getServer().getCommands().getDispatcher().execute(
                        "buyclaim",
                        player.createCommandSourceStack()
                );

                helper.assertValueEqual(result, 1, "debt-adjusted /buyclaim result");
                helper.assertValueEqual(backend.getExtraClaims(player), 2, "capacity after debt-adjusted purchase");
                helper.assertValueEqual(
                        InventoryPayment.count(player.getInventory(), Items.DIAMOND),
                        0L,
                        "payment after collecting carried debt"
                );

                PurchaseLedger.Account resultAccount = ledger.getAccountForTests(player.getUUID());
                helper.assertTrue(resultAccount != null, "debt test ledger result must exist");
                helper.assertValueEqual(resultAccount.paidClaims(), 2, "paid claims after debt purchase");
                helper.assertValueEqual(resultAccount.totalSpent(), 9L, "lifetime spent after debt purchase");
                helper.succeed();
            } catch (Exception exception) {
                BuyClaimChunks.LOGGER.error("Carried-debt GameTest failed", exception);
                helper.fail("Carried-debt GameTest failed: " + exception.getMessage());
            }
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void previousOverpaymentGrantsCompensationOnTheNextRealPurchase(GameTestHelper helper) {
        if (!hasRealBackend()) {
            helper.succeed();
            return;
        }

        ServerPlayer player = makeConnectedPlayer(helper, "bclaim-credit");
        helper.runAfterDelay(5, () -> {
            try {
                ClaimCapacityBackend backend = BuyClaimChunks.getClaimBackend();
                resetBackendClaims(helper, backend, player, 1);

                PurchaseLedger ledger = PurchaseLedger.get(player);
                PurchaseLedger.Account baseline = ledger.getOrCreateAccount(
                        player.getUUID(),
                        "minecraft:diamond",
                        1,
                        4L,
                        3.45D,
                        0.5D
                );
                PurchaseLedger.Account overpaid = new PurchaseLedger.Account("minecraft:diamond", 1, 9L);
                helper.assertTrue(
                        ledger.compareAndSet(player.getUUID(), baseline, overpaid),
                        "credit test ledger seed must succeed"
                );

                player.getInventory().clearContent();
                player.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 7));

                int result = helper.getLevel().getServer().getCommands().getDispatcher().execute(
                        "buyclaim",
                        player.createCommandSourceStack()
                );

                helper.assertValueEqual(result, 1, "credit-adjusted /buyclaim result");
                helper.assertValueEqual(
                        backend.getExtraClaims(player),
                        3,
                        "one compensation claim plus one requested claim"
                );
                helper.assertValueEqual(
                        InventoryPayment.count(player.getInventory(), Items.DIAMOND),
                        0L,
                        "payment after compensation purchase"
                );

                PurchaseLedger.Account resultAccount = ledger.getAccountForTests(player.getUUID());
                helper.assertTrue(resultAccount != null, "credit test ledger result must exist");
                helper.assertValueEqual(resultAccount.paidClaims(), 3, "paid claims after compensation purchase");
                helper.assertValueEqual(resultAccount.totalSpent(), 16L, "lifetime spent after compensation purchase");
                helper.succeed();
            } catch (Exception exception) {
                BuyClaimChunks.LOGGER.error("Compensation-credit GameTest failed", exception);
                helper.fail("Compensation-credit GameTest failed: " + exception.getMessage());
            }
        });
    }

    private static void resetBackendClaims(
            GameTestHelper helper,
            ClaimCapacityBackend backend,
            ServerPlayer player,
            int target
    ) {
        int observed = backend.getExtraClaims(player);
        if (observed == target) {
            return;
        }
        ClaimCapacityUpdate update = backend.setExtraClaims(player, observed, target);
        helper.assertTrue(update.success(), "backend test seed must succeed: " + update.detail());
    }

    private static boolean hasRealBackend() {
        String expected = System.getProperty(EXPECTED_BACKEND_PROPERTY, "");
        return expected.equals("ftb") || expected.equals("openpac");
    }

    private static ServerPlayer makeConnectedPlayer(GameTestHelper helper, String name) {
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(
                new GameProfile(UUID.randomUUID(), name),
                false
        );
        ServerPlayer player = new ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                cookie.gameProfile(),
                cookie.clientInformation()
        );

        Connection connection = new Connection(PacketFlow.SERVERBOUND) {
            @Override
            public void tick() {
                super.tick();
                player.resetLastActionTime();
            }

            @Override
            public boolean isMemoryConnection() {
                return true;
            }

            @Override
            public void send(Packet<?> packet, @Nullable PacketSendListener listener, boolean flush) {
                super.send(packet, listener, flush);
                if (packet instanceof ClientboundKeepAlivePacket keepAlivePacket) {
                    player.connection.handleKeepAlive(new ServerboundKeepAlivePacket(keepAlivePacket.getId()));
                }
            }
        };

        new EmbeddedChannel(connection);
        NetworkRegistry.configureMockConnection(connection);

        var server = helper.getLevel().getServer();
        server.getPlayerList().placeNewPlayer(connection, player, cookie);
        server.getConnection().getConnections().add(connection);
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        player.setYRot(180.0F);
        player.connection.chunkSender.sendNextChunks(player);
        player.connection.chunkSender.onChunkBatchReceivedByClient(64.0F);
        return player;
    }
}

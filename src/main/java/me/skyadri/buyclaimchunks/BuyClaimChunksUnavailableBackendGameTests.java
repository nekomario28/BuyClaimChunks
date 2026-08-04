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
public final class BuyClaimChunksUnavailableBackendGameTests {
    private static final String EXPECTED_BACKEND_PROPERTY = "buyclaimchunks.expectedBackend";

    private BuyClaimChunksUnavailableBackendGameTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void unavailableBackendRejectsPurchaseWithoutPayment(GameTestHelper helper) {
        String expectedEnvironment = System.getProperty(EXPECTED_BACKEND_PROPERTY, "");
        if (!expectedEnvironment.equals("none") && !expectedEnvironment.equals("both")) {
            helper.succeed();
            return;
        }

        final ServerPlayer player;
        try {
            player = makeConnectedPlayer(helper, GameType.SURVIVAL);
        } catch (RuntimeException exception) {
            BuyClaimChunks.LOGGER.error("Failed to create unavailable-backend GameTest player", exception);
            helper.fail("Failed to create unavailable-backend GameTest player: " + exception.getMessage());
            return;
        }

        helper.runAfterDelay(5, () -> {
            try {
                ClaimCapacityBackend backend = BuyClaimChunks.getClaimBackend();
                helper.assertValueEqual(backend.id(), "unavailable", "selected unavailable backend");

                player.getInventory().clearContent();
                player.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 4));
                helper.assertValueEqual(
                        InventoryPayment.count(player.getInventory(), Items.DIAMOND),
                        4L,
                        "payment before rejected purchase"
                );

                int commandResult = helper.getLevel().getServer().getCommands().getDispatcher().execute(
                        "buyclaim",
                        player.createCommandSourceStack()
                );

                helper.assertValueEqual(commandResult, 0, "rejected /buyclaim command result");
                helper.assertValueEqual(
                        InventoryPayment.count(player.getInventory(), Items.DIAMOND),
                        4L,
                        "payment after rejected purchase"
                );
                helper.succeed();
            } catch (Exception exception) {
                BuyClaimChunks.LOGGER.error("Unavailable-backend purchase rejection test failed", exception);
                helper.fail("Unavailable-backend purchase rejection test failed: " + exception.getMessage());
            }
        });
    }

    private static ServerPlayer makeConnectedPlayer(GameTestHelper helper, GameType gameType) {
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(
                new GameProfile(UUID.randomUUID(), "bclaim-unavailable"),
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
        player.gameMode.changeGameModeForPlayer(gameType);
        player.setYRot(180.0F);
        player.connection.chunkSender.sendNextChunks(player);
        player.connection.chunkSender.onChunkBatchReceivedByClient(64.0F);
        return player;
    }
}

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
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

@GameTestHolder(BuyClaimChunks.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BuyClaimChunksOpenPacPartyGameTests {
    private static final String PARTY_SEMANTICS_PROPERTY = "buyclaimchunks.openpacPartySemantics";
    private static final String PROBE_CLASS = "me.skyadri.buyclaimchunks.OpenPacPartySemanticsProbe";

    private BuyClaimChunksOpenPacPartyGameTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void openPacPartyOwnershipFollowsOwnerUuidWithoutLedgerMerging(GameTestHelper helper) {
        if (!Boolean.getBoolean(PARTY_SEMANTICS_PROPERTY)) {
            helper.succeed();
            return;
        }

        ServerPlayer owner = makeConnectedPlayer(helper, "bclaim-owner");
        ServerPlayer member = makeConnectedPlayer(helper, "bclaim-member");

        helper.runAfterDelay(10, () -> {
            try {
                Class<?> probeClass = Class.forName(PROBE_CLASS);
                Method run = probeClass.getDeclaredMethod(
                        "run",
                        GameTestHelper.class,
                        ServerPlayer.class,
                        ServerPlayer.class
                );
                run.setAccessible(true);
                run.invoke(null, helper, owner, member);
            } catch (InvocationTargetException exception) {
                Throwable cause = exception.getCause() == null ? exception : exception.getCause();
                BuyClaimChunks.LOGGER.error("OpenPAC party semantics probe failed", cause);
                helper.fail("OpenPAC party semantics probe failed: " + cause.getMessage());
            } catch (ReflectiveOperationException | RuntimeException exception) {
                BuyClaimChunks.LOGGER.error("Could not execute OpenPAC party semantics probe", exception);
                helper.fail("Could not execute OpenPAC party semantics probe: " + exception.getMessage());
            }
        });
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

package me.skyadri.buyclaimchunks;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.jetbrains.annotations.Nullable;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.player.config.api.v2.IPlayerConfigAPI;
import xaero.pac.common.server.player.config.api.v2.PlayerConfigOptions;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;

@GameTestHolder(BuyClaimChunks.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BuyClaimChunksOpenPacRestartIntegrationGameTests {
    private static final String PHASE_PROPERTY = "buyclaimchunks.integrationPhase";
    private static final String STATE_FILE_PROPERTY = "buyclaimchunks.integrationStateFile";
    private static final String SEED_PHASE = "seed";
    private static final String VERIFY_PHASE = "verify";

    private BuyClaimChunksOpenPacRestartIntegrationGameTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void purchaseUpdatesBonusAndConsumesPayment(GameTestHelper helper) {
        if (!isPhase(SEED_PHASE)) {
            helper.succeed();
            return;
        }

        final ServerPlayer player;
        try {
            player = makeConnectedPlayer(helper, GameType.SURVIVAL);
        } catch (RuntimeException exception) {
            BuyClaimChunks.LOGGER.error("Failed to create connected OpenPAC GameTest player", exception);
            helper.fail("Failed to create connected OpenPAC GameTest player: " + exception.getMessage());
            return;
        }

        helper.runAfterDelay(5, () -> {
            try {
                var server = helper.getLevel().getServer();
                OpenPACServerAPI api = OpenPACServerAPI.get(server);
                IPlayerConfigAPI playerConfig = api
                        .getPlayerConfigManager()
                        .getLoadedConfig(player.getUUID());

                helper.assertTrue(
                        playerConfig.isOptionAllowed(PlayerConfigOptions.BONUS_CHUNK_CLAIMS),
                        "BONUS_CHUNK_CLAIMS must be allowed for player configs"
                );

                IPlayerConfigAPI.SetResult resetResult = playerConfig.tryToSet(
                        PlayerConfigOptions.BONUS_CHUNK_CLAIMS,
                        0
                );
                helper.assertTrue(
                        resetResult == IPlayerConfigAPI.SetResult.SUCCESS
                                || resetResult == IPlayerConfigAPI.SetResult.DEFAULTED,
                        "OpenPAC bonus reset must succeed or resolve to its default"
                );

                helper.assertValueEqual(
                        BuyClaimChunks.getClaimBackend().getExtraClaims(player),
                        0,
                        "OpenPAC bonus claims before purchase"
                );
                helper.assertValueEqual(
                        api.getServerClaimsManager().getPlayerBaseClaimLimit(player),
                        0,
                        "OpenPAC base claim limit in all-paid integration config"
                );
                helper.assertValueEqual(
                        BuyClaimChunks.getClaimBackend().getFullClaimLimit(player),
                        0,
                        "OpenPAC full claim limit before purchase"
                );

                ResourceLocation itemId = Config.getItemRequired();
                Item requiredItem = itemId == null
                        ? null
                        : BuiltInRegistries.ITEM.getOptional(itemId).orElse(null);
                helper.assertTrue(requiredItem != null, "Configured payment item must exist");
                helper.assertTrue(requiredItem == Items.DIAMOND, "Fresh integration config must use diamonds");

                player.getInventory().clearContent();
                player.getInventory().setItem(0, new ItemStack(requiredItem, 1));
                player.getInventory().setItem(1, new ItemStack(requiredItem, 3));
                helper.assertValueEqual(
                        InventoryPayment.count(player.getInventory(), requiredItem),
                        4L,
                        "payment before OpenPAC purchase"
                );

                int commandResult = server.getCommands().getDispatcher().execute(
                        "buyclaim",
                        player.createCommandSourceStack()
                );

                helper.assertValueEqual(commandResult, 1, "/buyclaim command result");
                helper.assertValueEqual(
                        BuyClaimChunks.getClaimBackend().getExtraClaims(player),
                        1,
                        "OpenPAC bonus claims after purchase"
                );
                helper.assertValueEqual(
                        BuyClaimChunks.getClaimBackend().getFullClaimLimit(player),
                        1,
                        "OpenPAC full claim limit after purchase"
                );
                helper.assertValueEqual(
                        InventoryPayment.count(player.getInventory(), requiredItem),
                        0L,
                        "payment after OpenPAC purchase"
                );

                writeState(player.getUUID(), 1, 1, 4L, 0L);
                helper.succeed();
            } catch (Exception exception) {
                BuyClaimChunks.LOGGER.error("OpenPAC purchase seed phase failed", exception);
                helper.fail("OpenPAC purchase seed phase failed: " + exception.getMessage());
            }
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void bonusQuotaSurvivesServerRestart(GameTestHelper helper) {
        if (!isPhase(VERIFY_PHASE)) {
            helper.succeed();
            return;
        }

        try {
            Properties state = readState();
            UUID playerId = UUID.fromString(requiredProperty(state, "playerUuid"));
            int expectedBonus = Integer.parseInt(requiredProperty(state, "expectedBonus"));
            int expectedFullLimit = Integer.parseInt(requiredProperty(state, "expectedFullLimit"));

            helper.assertValueEqual(
                    Long.parseLong(requiredProperty(state, "paymentBefore")),
                    4L,
                    "recorded payment before OpenPAC purchase"
            );
            helper.assertValueEqual(
                    Long.parseLong(requiredProperty(state, "paymentAfter")),
                    0L,
                    "recorded payment after OpenPAC purchase"
            );

            OpenPACServerAPI api = OpenPACServerAPI.get(helper.getLevel().getServer());
            IPlayerConfigAPI playerConfig = api
                    .getPlayerConfigManager()
                    .getLoadedConfig(playerId);
            int loadedBonus = playerConfig.getEffective(PlayerConfigOptions.BONUS_CHUNK_CLAIMS);
            int loadedBase = api.getServerClaimsManager().getPlayerBaseClaimLimit(playerId);

            helper.assertValueEqual(
                    loadedBonus,
                    expectedBonus,
                    "OpenPAC bonus claims loaded after restart"
            );
            helper.assertValueEqual(
                    loadedBase,
                    0,
                    "OpenPAC base claim limit after restart"
            );
            helper.assertValueEqual(
                    loadedBase + loadedBonus,
                    expectedFullLimit,
                    "OpenPAC derived full claim limit loaded after restart"
            );
            helper.succeed();
        } catch (Exception exception) {
            BuyClaimChunks.LOGGER.error("OpenPAC restart verification phase failed", exception);
            helper.fail("OpenPAC restart verification phase failed: " + exception.getMessage());
        }
    }

    private static ServerPlayer makeConnectedPlayer(GameTestHelper helper, GameType gameType) {
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(
                new GameProfile(UUID.randomUUID(), "bclaim-opac-test"),
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

    private static boolean isPhase(String expected) {
        return expected.equals(System.getProperty(PHASE_PROPERTY, ""));
    }

    private static Path stateFile() {
        String configured = System.getProperty(STATE_FILE_PROPERTY);
        if (configured == null || configured.isBlank()) {
            throw new IllegalStateException("Missing system property " + STATE_FILE_PROPERTY);
        }
        return Path.of(configured);
    }

    private static void writeState(
            UUID playerId,
            int expectedBonus,
            int expectedFullLimit,
            long paymentBefore,
            long paymentAfter
    ) throws IOException {
        Properties state = new Properties();
        state.setProperty("playerUuid", playerId.toString());
        state.setProperty("expectedBonus", Integer.toString(expectedBonus));
        state.setProperty("expectedFullLimit", Integer.toString(expectedFullLimit));
        state.setProperty("paymentBefore", Long.toString(paymentBefore));
        state.setProperty("paymentAfter", Long.toString(paymentAfter));

        Path path = stateFile();
        Files.createDirectories(path.getParent());
        try (OutputStream output = Files.newOutputStream(path)) {
            state.store(output, "BuyClaimChunks OpenPAC restart integration state");
        }
    }

    private static Properties readState() throws IOException {
        Path path = stateFile();
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException("Integration state file is missing: " + path);
        }

        Properties state = new Properties();
        try (InputStream input = Files.newInputStream(path)) {
            state.load(input);
        }
        return state;
    }

    private static String requiredProperty(Properties state, String key) {
        String value = state.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Integration state is missing property " + key);
        }
        return value;
    }
}

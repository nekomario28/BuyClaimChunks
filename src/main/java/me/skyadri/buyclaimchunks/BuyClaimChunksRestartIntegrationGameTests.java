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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;

@GameTestHolder(BuyClaimChunks.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BuyClaimChunksRestartIntegrationGameTests {
    private static final String EXPECTED_BACKEND_PROPERTY = "buyclaimchunks.expectedBackend";
    private static final String PHASE_PROPERTY = "buyclaimchunks.integrationPhase";
    private static final String STATE_FILE_PROPERTY = "buyclaimchunks.integrationStateFile";
    private static final String SEED_PHASE = "seed";
    private static final String VERIFY_PHASE = "verify";

    private BuyClaimChunksRestartIntegrationGameTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void purchaseUpdatesCapacityAndConsumesPayment(GameTestHelper helper) {
        if (!isPhase(SEED_PHASE)) {
            helper.succeed();
            return;
        }

        final ServerPlayer player;
        try {
            player = makeConnectedPlayer(helper, GameType.SURVIVAL, UUID.randomUUID(), "bclaim-test");
        } catch (RuntimeException exception) {
            BuyClaimChunks.LOGGER.error("Failed to create connected GameTest player", exception);
            helper.fail("Failed to create connected GameTest player: " + exception.getMessage());
            return;
        }

        helper.runAfterDelay(5, () -> {
            try {
                var server = helper.getLevel().getServer();
                ClaimCapacityBackend backend = BuyClaimChunks.getClaimBackend();
                String expectedBackend = expectedBackend();

                helper.assertValueEqual(backend.id(), expectedBackend, "selected claim backend");

                int observed = backend.getExtraClaims(player);
                if (observed != 0) {
                    ClaimCapacityUpdate reset = backend.setExtraClaims(player, observed, 0);
                    helper.assertTrue(reset.success(), "Backend capacity reset must succeed: " + reset.detail());
                }

                helper.assertValueEqual(backend.getExtraClaims(player), 0, "extra claims before purchase");
                helper.assertValueEqual(backend.getFullClaimLimit(player), 0, "full claim limit before purchase");

                ResourceLocation itemId = Config.getItemRequired();
                Item requiredItem = itemId == null
                        ? null
                        : BuiltInRegistries.ITEM.getOptional(itemId).orElse(null);
                helper.assertTrue(requiredItem != null, "Configured payment item must exist");
                helper.assertTrue(requiredItem == Items.DIAMOND, "Fresh integration config must use diamonds");

                long price = PricingCalculator.totalPrice(
                        0,
                        1,
                        Config.getAmountRequired(),
                        Config.getPriceGrowthFactor(),
                        Config.getPriceExponent()
                );
                helper.assertValueEqual(price, 4L, "fresh-config first purchase price");

                player.getInventory().clearContent();
                player.getInventory().setItem(0, new ItemStack(requiredItem, 1));
                player.getInventory().setItem(1, new ItemStack(requiredItem, 3));
                helper.assertValueEqual(
                        InventoryPayment.count(player.getInventory(), requiredItem),
                        4L,
                        "payment before end-to-end purchase"
                );

                int commandResult = server.getCommands().getDispatcher().execute(
                        "buyclaim",
                        player.createCommandSourceStack()
                );

                helper.assertValueEqual(commandResult, 1, "/buyclaim command result");
                helper.assertValueEqual(backend.getExtraClaims(player), 1, "extra claims after purchase");
                helper.assertValueEqual(backend.getFullClaimLimit(player), 1, "full claim limit after purchase");
                helper.assertValueEqual(
                        InventoryPayment.count(player.getInventory(), requiredItem),
                        0L,
                        "payment after end-to-end purchase"
                );

                writeState(expectedBackend, player.getUUID(), 1, 1, 4L, 0L);
                helper.succeed();
            } catch (Exception exception) {
                BuyClaimChunks.LOGGER.error("Claim purchase seed phase failed", exception);
                helper.fail("Claim purchase seed phase failed: " + exception.getMessage());
            }
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void capacitySurvivesServerRestart(GameTestHelper helper) {
        if (!isPhase(VERIFY_PHASE)) {
            helper.succeed();
            return;
        }

        try {
            Properties state = readState();
            String expectedBackend = requiredProperty(state, "backend");
            UUID playerId = UUID.fromString(requiredProperty(state, "playerUuid"));
            int expectedClaims = Integer.parseInt(requiredProperty(state, "expectedClaims"));
            int expectedFullLimit = Integer.parseInt(requiredProperty(state, "expectedFullLimit"));

            helper.assertValueEqual(expectedBackend(), expectedBackend, "restart test backend property");
            helper.assertValueEqual(
                    Long.parseLong(requiredProperty(state, "paymentBefore")),
                    4L,
                    "recorded payment before seed purchase"
            );
            helper.assertValueEqual(
                    Long.parseLong(requiredProperty(state, "paymentAfter")),
                    0L,
                    "recorded payment after seed purchase"
            );

            ServerPlayer player = makeConnectedPlayer(helper, GameType.SURVIVAL, playerId, "bclaim-reload");
            helper.runAfterDelay(5, () -> {
                try {
                    ClaimCapacityBackend backend = BuyClaimChunks.getClaimBackend();
                    helper.assertValueEqual(backend.id(), expectedBackend, "selected backend after restart");
                    helper.assertValueEqual(
                            backend.getExtraClaims(player),
                            expectedClaims,
                            "extra claims loaded after restart"
                    );
                    helper.assertValueEqual(
                            backend.getFullClaimLimit(player),
                            expectedFullLimit,
                            "full claim limit loaded after restart"
                    );
                    helper.succeed();
                } catch (Exception exception) {
                    BuyClaimChunks.LOGGER.error("Claim restart verification phase failed", exception);
                    helper.fail("Claim restart verification phase failed: " + exception.getMessage());
                }
            });
        } catch (Exception exception) {
            BuyClaimChunks.LOGGER.error("Failed to prepare restart verification phase", exception);
            helper.fail("Failed to prepare restart verification phase: " + exception.getMessage());
        }
    }

    private static ServerPlayer makeConnectedPlayer(
            GameTestHelper helper,
            GameType gameType,
            UUID playerId,
            String playerName
    ) {
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(
                new GameProfile(playerId, playerName),
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

    private static String expectedBackend() {
        String backend = System.getProperty(EXPECTED_BACKEND_PROPERTY, "");
        if (!backend.equals("ftb") && !backend.equals("openpac")) {
            throw new IllegalStateException("Expected test backend must be ftb or openpac, got: " + backend);
        }
        return backend;
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
            String backend,
            UUID playerId,
            int expectedClaims,
            int expectedFullLimit,
            long paymentBefore,
            long paymentAfter
    ) throws IOException {
        Properties state = new Properties();
        state.setProperty("backend", backend);
        state.setProperty("playerUuid", playerId.toString());
        state.setProperty("expectedClaims", Integer.toString(expectedClaims));
        state.setProperty("expectedFullLimit", Integer.toString(expectedFullLimit));
        state.setProperty("paymentBefore", Long.toString(paymentBefore));
        state.setProperty("paymentAfter", Long.toString(paymentAfter));

        Path path = stateFile();
        Files.createDirectories(path.getParent());
        try (OutputStream output = Files.newOutputStream(path)) {
            state.store(output, "BuyClaimChunks restart integration state");
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

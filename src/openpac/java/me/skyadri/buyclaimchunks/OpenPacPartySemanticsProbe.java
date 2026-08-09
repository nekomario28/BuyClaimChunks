package me.skyadri.buyclaimchunks;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import xaero.pac.common.claims.player.api.IPlayerChunkClaimAPI;
import xaero.pac.common.parties.party.member.PartyMemberRank;
import xaero.pac.common.parties.party.member.api.IPartyMemberAPI;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.claims.api.IServerClaimsManagerAPI;
import xaero.pac.common.server.parties.party.api.IPartyManagerAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

import java.util.UUID;

/**
 * OpenPAC-specific integration probe. This class is deliberately not annotated
 * as a GameTest so FTB-only runs do not need to load any OpenPAC API classes.
 * The common GameTest entrypoint invokes it reflectively only in the dedicated
 * OpenPAC party-semantics run.
 */
final class OpenPacPartySemanticsProbe {
    private OpenPacPartySemanticsProbe() {
    }

    static void run(GameTestHelper helper, ServerPlayer originalOwner, ServerPlayer member) throws Exception {
        ClaimCapacityBackend backend = BuyClaimChunks.getClaimBackend();
        helper.assertValueEqual(backend.id(), "openpac", "party semantics backend");

        OpenPACServerAPI api = OpenPACServerAPI.get(helper.getLevel().getServer());
        IPartyManagerAPI partyManager = api.getPartyManager();
        IServerClaimsManagerAPI claimsManager = api.getServerClaimsManager();

        resetBonus(helper, backend, originalOwner, 0);
        resetBonus(helper, backend, member, 0);

        IServerPartyAPI party = partyManager.createPartyForOwner(originalOwner);
        helper.assertTrue(party != null, "OpenPAC test party must be created");
        UUID partyId = party.getId();

        IPartyMemberAPI addedMember = party.addMember(
                member.getUUID(),
                PartyMemberRank.ADMIN,
                member.getGameProfile().getName()
        );
        helper.assertTrue(addedMember != null, "OpenPAC test member must join the party as admin");

        ClaimCapacityContext ownerContext = backend.getCapacityContext(originalOwner);
        ClaimCapacityContext memberContext = backend.getCapacityContext(member);
        helper.assertTrue(
                ownerContext.kind() == ClaimCapacityContext.Kind.PARTY_OWNER_SHARED,
                "owner purchase context must be PARTY_OWNER_SHARED"
        );
        helper.assertTrue(
                memberContext.kind() == ClaimCapacityContext.Kind.PARTY_MEMBER_PERSONAL,
                "member purchase context must be PARTY_MEMBER_PERSONAL"
        );
        helper.assertValueEqual(memberContext.partyOwnerId(), originalOwner.getUUID(), "member party owner UUID");
        helper.assertValueEqual(ownerContext.partyId(), partyId, "owner party ID");
        helper.assertValueEqual(memberContext.partyId(), partyId, "member party ID");

        // A member's /buyclaim remains bound to that member's player UUID.
        payAndBuy(helper, member, 1, 4);
        helper.assertValueEqual(backend.getExtraClaims(member), 1, "member personal bonus after /buyclaim");
        helper.assertValueEqual(backend.getExtraClaims(originalOwner), 0, "owner bonus unaffected by member /buyclaim");

        // The owner buys the quota that OpenPAC PARTY mode will actually use.
        payAndBuy(helper, originalOwner, 1, 4);
        helper.assertValueEqual(backend.getExtraClaims(originalOwner), 1, "owner bonus after /buyclaim");

        PurchaseLedger ledger = PurchaseLedger.get(originalOwner);
        assertLedger(helper, ledger, originalOwner.getUUID(), 1, 4L, "owner ledger before claims");
        assertLedger(helper, ledger, member.getUUID(), 1, 4L, "member ledger before claims");

        ResourceLocation dimension = helper.getLevel().dimension().location();
        int baseBlockX = member.blockPosition().getX();
        int baseBlockZ = member.blockPosition().getZ();

        // PARTY mode forces the member's claim to the primary party owner's UUID.
        int partyClaimResult = execute(
                member,
                "openpac-claims party claim " + baseBlockX + " " + baseBlockZ
        );
        helper.assertValueEqual(partyClaimResult, 1, "member PARTY-mode claim result");
        IPlayerChunkClaimAPI partyClaim = claimAt(claimsManager, dimension, baseBlockX, baseBlockZ);
        helper.assertTrue(partyClaim != null, "member PARTY-mode claim must exist");
        helper.assertValueEqual(
                partyClaim.getPlayerId(),
                originalOwner.getUUID(),
                "member PARTY-mode claim owner UUID"
        );
        helper.assertValueEqual(
                claimsManager.getPlayerInfo(originalOwner.getUUID()).getClaimCount(),
                1,
                "owner UUID claim count after member PARTY claim"
        );
        helper.assertValueEqual(
                claimsManager.getPlayerInfo(member.getUUID()).getClaimCount(),
                0,
                "member UUID claim count after member PARTY claim"
        );

        // PLAYER mode still consumes the member's own separately purchased pool.
        int personalBlockX = baseBlockX + 16;
        int personalClaimResult = execute(
                member,
                "openpac-claims player claim " + personalBlockX + " " + baseBlockZ
        );
        helper.assertValueEqual(personalClaimResult, 1, "member PLAYER-mode claim result");
        IPlayerChunkClaimAPI personalClaim = claimAt(claimsManager, dimension, personalBlockX, baseBlockZ);
        helper.assertTrue(personalClaim != null, "member PLAYER-mode claim must exist");
        helper.assertValueEqual(personalClaim.getPlayerId(), member.getUUID(), "member PLAYER-mode owner UUID");
        helper.assertValueEqual(
                claimsManager.getPlayerInfo(originalOwner.getUUID()).getClaimCount(),
                1,
                "owner UUID count remains one"
        );
        helper.assertValueEqual(
                claimsManager.getPlayerInfo(member.getUUID()).getClaimCount(),
                1,
                "member UUID count becomes one"
        );

        // Leaving a party must not move either player's purchased quota, ledger,
        // or already-created claims. Only membership/permission context changes.
        helper.assertTrue(party.removeMember(member.getUUID()) != null, "member must leave test party");
        helper.assertTrue(
                backend.getCapacityContext(member).kind() == ClaimCapacityContext.Kind.PERSONAL,
                "former member purchase context must become PERSONAL"
        );
        helper.assertValueEqual(backend.getExtraClaims(originalOwner), 1, "owner bonus after member leaves");
        helper.assertValueEqual(backend.getExtraClaims(member), 1, "member bonus after leaving");
        assertLedger(helper, ledger, originalOwner.getUUID(), 1, 4L, "owner ledger after member leaves");
        assertLedger(helper, ledger, member.getUUID(), 1, 4L, "member ledger after leaving");
        helper.assertValueEqual(
                claimAt(claimsManager, dimension, baseBlockX, baseBlockZ).getPlayerId(),
                originalOwner.getUUID(),
                "party claim owner after member leaves"
        );
        helper.assertValueEqual(
                claimAt(claimsManager, dimension, personalBlockX, baseBlockZ).getPlayerId(),
                member.getUUID(),
                "personal claim owner after member leaves"
        );

        // Rejoin and transfer ownership through OpenPAC's real transfer command.
        IPartyMemberAPI rejoinedMember = party.addMember(
                member.getUUID(),
                PartyMemberRank.ADMIN,
                member.getGameProfile().getName()
        );
        helper.assertTrue(rejoinedMember != null, "member must rejoin as admin before transfer");

        int transferResult = execute(
                originalOwner,
                "openpac-parties transfer " + member.getGameProfile().getName() + " confirm"
        );
        helper.assertValueEqual(transferResult, 1, "OpenPAC party owner transfer result");

        IServerPartyAPI transferredParty = partyManager.getPartyById(partyId);
        helper.assertTrue(transferredParty != null, "party must survive owner transfer");
        helper.assertValueEqual(transferredParty.getId(), partyId, "party ID across owner transfer");
        helper.assertValueEqual(
                transferredParty.getOwner().getUUID(),
                member.getUUID(),
                "new OpenPAC party owner"
        );

        // OpenPAC transfer changes the party owner mapping, not per-player BONUS
        // values or BuyClaimChunks' per-player economic histories.
        helper.assertValueEqual(backend.getExtraClaims(originalOwner), 1, "old owner bonus after transfer");
        helper.assertValueEqual(backend.getExtraClaims(member), 1, "new owner bonus after transfer");
        assertLedger(helper, ledger, originalOwner.getUUID(), 1, 4L, "old owner ledger after transfer");
        assertLedger(helper, ledger, member.getUUID(), 1, 4L, "new owner ledger after transfer");
        helper.assertTrue(
                backend.getCapacityContext(originalOwner).kind() == ClaimCapacityContext.Kind.PARTY_MEMBER_PERSONAL,
                "old owner must become member-personal context"
        );
        helper.assertTrue(
                backend.getCapacityContext(member).kind() == ClaimCapacityContext.Kind.PARTY_OWNER_SHARED,
                "new owner must get owner-shared context"
        );

        // Existing claims are not rewritten by the transfer command. The old
        // PARTY claim remains owned by the old owner UUID, while the new owner
        // already has their earlier PLAYER claim under their own UUID.
        helper.assertValueEqual(
                claimAt(claimsManager, dimension, baseBlockX, baseBlockZ).getPlayerId(),
                originalOwner.getUUID(),
                "existing party claim owner after owner transfer"
        );
        helper.assertValueEqual(
                claimAt(claimsManager, dimension, personalBlockX, baseBlockZ).getPlayerId(),
                member.getUUID(),
                "new owner's pre-transfer personal claim owner"
        );
        helper.assertValueEqual(
                claimsManager.getPlayerInfo(originalOwner.getUUID()).getClaimCount(),
                1,
                "old owner count after transfer"
        );
        helper.assertValueEqual(
                claimsManager.getPlayerInfo(member.getUUID()).getClaimCount(),
                1,
                "new owner count after transfer"
        );

        // The new owner's previous personal claim and future PARTY claims share
        // the same UUID pool. Buy one more slot, then use it for a PARTY claim.
        payAndBuy(helper, member, 1, 5);
        helper.assertValueEqual(backend.getExtraClaims(member), 2, "new owner bonus after second purchase");
        assertLedger(helper, ledger, member.getUUID(), 2, 9L, "new owner ledger after second purchase");

        int newOwnerPartyBlockX = baseBlockX + 32;
        int newOwnerPartyClaimResult = execute(
                member,
                "openpac-claims party claim " + newOwnerPartyBlockX + " " + baseBlockZ
        );
        helper.assertValueEqual(newOwnerPartyClaimResult, 1, "new owner PARTY-mode claim result");
        IPlayerChunkClaimAPI newOwnerPartyClaim = claimAt(
                claimsManager,
                dimension,
                newOwnerPartyBlockX,
                baseBlockZ
        );
        helper.assertTrue(newOwnerPartyClaim != null, "new owner PARTY claim must exist");
        helper.assertValueEqual(
                newOwnerPartyClaim.getPlayerId(),
                member.getUUID(),
                "new owner PARTY claim UUID"
        );
        helper.assertValueEqual(
                claimsManager.getPlayerInfo(member.getUUID()).getClaimCount(),
                2,
                "new owner's personal and PARTY claims share one count"
        );

        BuyClaimChunks.LOGGER.info(
                "OpenPAC party semantics verified: member purchases stay personal; PARTY claims use the owner UUID; leaving does not move quota/ledger; owner transfer preserves UUID-bound bonus, ledger, and existing claim ownership."
        );

        // Clean up direct OpenPAC state so other GameTests in this JVM do not
        // inherit claims or party membership from this probe.
        claimsManager.unclaim(dimension, baseBlockX >> 4, baseBlockZ >> 4);
        claimsManager.unclaim(dimension, personalBlockX >> 4, baseBlockZ >> 4);
        claimsManager.unclaim(dimension, newOwnerPartyBlockX >> 4, baseBlockZ >> 4);
        partyManager.removePartyById(partyId);
        helper.succeed();
    }

    private static void payAndBuy(
            GameTestHelper helper,
            ServerPlayer player,
            int amount,
            int diamonds
    ) throws Exception {
        player.getInventory().clearContent();
        player.getInventory().setItem(0, new ItemStack(Items.DIAMOND, diamonds));
        String command = amount == 1 ? "buyclaim" : "buyclaim " + amount;
        int result = execute(player, command);
        helper.assertValueEqual(result, 1, command + " result for " + player.getGameProfile().getName());
        helper.assertValueEqual(
                InventoryPayment.count(player.getInventory(), Items.DIAMOND),
                0L,
                "payment consumed for " + player.getGameProfile().getName()
        );
    }

    private static int execute(ServerPlayer player, String command) throws Exception {
        return player.getServer().getCommands().getDispatcher().execute(
                command,
                player.createCommandSourceStack()
        );
    }

    private static IPlayerChunkClaimAPI claimAt(
            IServerClaimsManagerAPI claimsManager,
            ResourceLocation dimension,
            int blockX,
            int blockZ
    ) {
        return claimsManager.get(dimension, blockX >> 4, blockZ >> 4);
    }

    private static void resetBonus(
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
        helper.assertTrue(update.success(), "OpenPAC bonus reset must succeed: " + update.detail());
    }

    private static void assertLedger(
            GameTestHelper helper,
            PurchaseLedger ledger,
            UUID playerId,
            int paidClaims,
            long totalSpent,
            String label
    ) {
        PurchaseLedger.Account account = ledger.getAccountForTests(playerId);
        helper.assertTrue(account != null, label + " must exist");
        helper.assertValueEqual(account.currencyItemId(), "minecraft:diamond", label + " currency");
        helper.assertValueEqual(account.paidClaims(), paidClaims, label + " paid claims");
        helper.assertValueEqual(account.totalSpent(), totalSpent, label + " total spent");
    }
}

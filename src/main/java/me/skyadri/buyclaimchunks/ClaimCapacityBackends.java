package me.skyadri.buyclaimchunks;

import net.neoforged.fml.ModList;

public final class ClaimCapacityBackends {
    static final String FTB_MOD_ID = "ftbchunks";
    static final String OPENPAC_MOD_ID = "openpartiesandclaims";

    private ClaimCapacityBackends() {
    }

    public static ClaimCapacityBackend create() {
        boolean ftbLoaded = ModList.get().isLoaded(FTB_MOD_ID);
        boolean openPacLoaded = ModList.get().isLoaded(OPENPAC_MOD_ID);

        if (ftbLoaded && !openPacLoaded) {
            return new FtbClaimCapacityBackend();
        }
        if (openPacLoaded && !ftbLoaded) {
            return new OpenPacClaimCapacityBackend();
        }

        String reason;
        if (ftbLoaded) {
            reason = "Both FTB Chunks and Open Parties and Claims are installed. "
                    + "Install exactly one claim backend; /buyclaim is disabled to avoid updating the wrong quota.";
        } else {
            reason = "No supported claim backend is installed. Install exactly one of FTB Chunks or "
                    + "Open Parties and Claims; /buyclaim is disabled.";
        }

        BuyClaimChunks.LOGGER.error(reason);
        return new UnavailableClaimCapacityBackend(reason);
    }
}

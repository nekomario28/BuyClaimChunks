# Universal FTB Chunks / OpenPAC backend and repricing-ledger plan

## Baseline and target

- Repository: `nekomario28/BuyClaimChunks`
- Baseline: `main` at `22d7adcbe5f711a3bc7e2cb8593c60e19838dce1`
- Previous release: `1.1.1`
- Target release: `1.2.0`
- Platform: Minecraft `1.21.1`, NeoForge `21.1.x`, Java `21`

## Goal

Publish one BuyClaimChunks Continued JAR that provides the same player-visible economy with either FTB Chunks or Open Parties and Claims, and safely reconciles later numeric price-curve changes without confiscating existing claims.

```text
buyclaimchunks-continued-neoforge-1.21.1-1.2.0.jar
```

Server operators install exactly one claim backend. The universal JAR detects the installed backend at runtime.

| Installed backend | Selection |
|---|---|
| FTB Chunks only | FTB adapter |
| OpenPAC only | OpenPAC adapter |
| neither | unavailable backend; purchases disabled |
| both | unavailable backend; purchases disabled |

Both/neither configurations must fail closed without crashing the server or consuming payment.

## Feature-parity contract

- `/buyclaim` buys one personal extra-claim slot.
- `/buyclaim <amount>` buys multiple sequentially priced slots.
- The command remains player-only.
- Both backends use the same `config/buyclaimchunks-common.toml` keys and defaults.
- BuyClaimChunks-paid capacity determines the price-curve position.
- Administrator-granted backend capacity does not create purchase-history credit, but still counts toward `maxExtraClaims`.
- The server stores currency ID, paid-capacity count, and lifetime consumed amount per player UUID.
- Numeric price increases never remove existing claims; cumulative shortfall is carried into the next purchase.
- Numeric price decreases grant compensation capacity supported by previous payments, bounded by remaining backend capacity.
- Changing `itemRequired` starts a new same-capacity baseline because different currencies have no implicit exchange rate.
- Payment uses the normal 36-slot inventory including hotbar, excluding armor and offhand.
- Backend capacity and purchase-ledger state are updated and verified before payment is consumed.
- Concurrent changes reject the transaction instead of being overwritten.
- Failed updates consume no items.
- Unexpected payment failure attempts verified rollback of both capacity and ledger state.
- FTB Chunks or OpenPAC remains the source of truth for current capacity; the additional ledger stores economic history only.
- Successful capacity and ledger updates survive normal shutdown and a second-JVM restart.
- Automatic claiming, forceload sales, upkeep, unclaim refunds, transfers, and party-shared purchases remain out of scope.

## Default economy

```toml
[general]
itemRequired = "minecraft:diamond"
amountRequired = 4
priceGrowthFactor = 3.45
priceExponent = 0.5
maxExtraClaims = 100
maxPurchaseAmount = 100
```

Per-slot price for one-based paid slot `n`:

```text
round(amountRequired + priceGrowthFactor * (n ^ priceExponent - 1))
```

Future same-currency purchases use:

```text
next payment
= active-curve cumulative cost through resulting paid capacity
- lifetime consumed amount
```

Existing config files are retained. The documentation must explain the stop-edit-save-restart-test workflow, debt/credit behavior, currency-change baseline, legacy-world baseline, and the old `amountRequired = 1` migration case.

## Architecture

### Shared core

Backend-independent code owns:

- command registration and validation;
- per-slot and cumulative pricing;
- lifetime-payment debt/credit calculation;
- inventory counting and consumption;
- purchase-ledger compare-and-set persistence;
- transaction ordering and dual rollback;
- configuration, messages, and logging.

Backend contract:

```java
interface ClaimCapacityBackend {
    String id();
    int getExtraClaims(ServerPlayer player);
    int getFullClaimLimit(ServerPlayer player);
    ClaimCapacityUpdate setExtraClaims(
        ServerPlayer player,
        int expectedCurrent,
        int newValue
    );
}
```

### Purchase ledger

World `SavedData` stores one account per player UUID:

```text
currencyItemId
paidClaims
totalSpent
schemaVersion
```

The ledger does not replace backend quota data. It records only the economic history required to calculate future debt or credit.

For a world created before the ledger, the first account treats current backend extra capacity as paid at the active curve. This preserves the old next-price position and begins exact tracking from the upgrade point. Because legacy backend data cannot distinguish purchases from administrator grants, this is explicitly documented as a compatibility baseline.

### Runtime selector

The common factory uses NeoForge `ModList` and returns exactly one of:

- `FtbClaimCapacityBackend`
- `OpenPacClaimCapacityBackend`
- `UnavailableClaimCapacityBackend`

The unavailable adapter prevents class-selection ambiguity and makes `/buyclaim` return a no-charge backend-unavailable error.

### FTB adapter

- Read FTB personal extra claims.
- Set the absolute value through the FTB administrative command path.
- Compare before write and re-read after write.
- Preserve all behavior and persisted player data from 1.1.1.

### OpenPAC adapter

- Read and write `PlayerConfigOptions.BONUS_CHUNK_CLAIMS` through the v2 server API.
- Re-read the bonus after writes.
- Derive the 0.27.6-compatible full limit as base plus bonus.
- Warn when the effective base is non-zero.
- Reject illegal, non-writable, concurrent, rejected, or unpersisted writes without payment.

## OpenPAC all-paid model

The documented intended configuration is:

```text
effective base claim limit = 0
party/member bonus = 0
party-owner bonus = 0
permission/rank override = disabled or 0
purchased capacity = BONUS_CHUNK_CLAIMS
```

The mod must warn but never silently mutate OpenPAC configuration.

## Transaction order

1. Read backend capacity and the active ledger snapshot.
2. Validate requested amount and remaining total capacity.
3. Calculate carried debt, available compensation credit, requested capacity, and required payment.
4. Validate inventory payment.
5. Compare-before-write and verify backend capacity.
6. Compare-and-set the ledger from the quoted snapshot to the result.
7. Consume payment.
8. If payment unexpectedly fails, restore both backend capacity and the ledger snapshot.
9. Report success only after all three states are confirmed.

## Packaging and license boundary

The universal JAR contains both thin adapters and the project-owned economic ledger, but does not contain FTB Chunks or OpenPAC classes/assets.

- FTB Chunks is an external All Rights Reserved / visible-source dependency.
- OpenPAC is an external LGPL-3.0-only dependency.
- BuyClaimChunks Continued remains MIT-licensed.
- Both APIs are compile-time-only dependencies.
- CI installs full backends only in isolated runtime configurations.
- `LICENSE`, `NOTICE`, and `THIRD_PARTY_NOTICES.md` must be packaged.
- NeoForge metadata declares both claim mods as optional alternatives.

## Test and CI gates

### Unit tests

- published default per-slot and batch examples;
- cumulative-price and affordable-capacity inverse behavior;
- price increase carries cumulative shortfall into the next purchase;
- price decrease grants supported compensation capacity;
- credit limited by remaining capacity carries forward;
- overflow and invalid-input rejection.

### Universal artifact

- exactly one non-sources JAR;
- universal file name;
- both adapter classes, unavailable adapter, repricing result, and purchase ledger present;
- no FTB/OpenPAC dependency classes embedded;
- both dependencies declared optional;
- required license and notice files present.

### FTB environment

- unit/shared GameTests;
- selected backend is `ftb`;
- real purchase `0 -> 1`;
- payment `4 -> 0`;
- ledger records diamond currency, paid capacity `1`, and total spent `4`;
- normal shutdown and second-JVM reload of both FTB capacity and ledger state;
- dedicated server reaches `Done`.

### OpenPAC environment

- unit/shared GameTests;
- selected backend is `openpac`;
- real bonus purchase `0 -> 1`;
- zero-base full limit `0 -> 1`;
- payment `4 -> 0`;
- ledger records diamond currency, paid capacity `1`, and total spent `4`;
- normal shutdown and second-JVM reload of both OpenPAC capacity and ledger state;
- dedicated server reaches `Done`.

### Misconfiguration guards

- no backend: `/buyclaim` rejects with payment unchanged and server reaches `Done`;
- both backends: `/buyclaim` rejects with payment unchanged and server reaches `Done`;
- neither case may silently choose a backend or create a successful paid transaction.

## Documentation and publication

Required release material:

- English and Japanese READMEs;
- exact defaults and setting-change steps;
- English and Japanese repricing-ledger guides;
- OpenPAC zero-base setup and migration guides;
- universal-JAR license review;
- bilingual Modrinth project description;
- Modrinth 1.2.0 changelog and metadata template;
- guarded upload script requiring a validated SHA-256;
- one-JAR GitHub Release workflow.

OpenPAC can be linked as an optional Modrinth dependency. FTB Chunks remains an external official FTB/CurseForge installation when no official Modrinth project can be resolved.

## Definition of done

The work is complete when one universal artifact exposes identical commands, settings, price-reconciliation behavior, limits, payment safety, and restart persistence with either supported backend; never confiscates existing claims after a price increase; safely disables purchases with both or neither backend; passes every self-hosted CI gate; and is ready for explicit merge and release authorization.

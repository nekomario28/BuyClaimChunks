# Universal FTB Chunks / OpenPAC backend plan

## Baseline and target

- Repository: `nekomario28/BuyClaimChunks`
- Baseline: `main` at `22d7adcbe5f711a3bc7e2cb8593c60e19838dce1`
- Previous release: `1.1.1`
- Target release: `1.2.0`
- Platform: Minecraft `1.21.1`, NeoForge `21.1.x`, Java `21`

## Goal

Publish one BuyClaimChunks Continued JAR that provides the same player-visible economy with either FTB Chunks or Open Parties and Claims.

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
- Current backend-owned extra capacity determines price and `maxExtraClaims` position.
- Administrator-granted capacity counts.
- Payment uses the normal 36-slot inventory including hotbar, excluding armor and offhand.
- Capacity is updated and verified before payment is consumed.
- Concurrent changes reject the transaction instead of being overwritten.
- Failed updates consume no items.
- Unexpected payment failure attempts a verified capacity rollback.
- No independent quota or purchase-count database is introduced.
- Successful purchases survive normal shutdown and a second-JVM restart.
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

Per-slot price for one-based slot `n`:

```text
round(amountRequired + priceGrowthFactor * (n ^ priceExponent - 1))
```

Existing config files are retained. The documentation must explain the stop-edit-save-restart-test workflow and the old `amountRequired = 1` migration case.

## Architecture

### Shared core

Backend-independent code owns:

- command registration and validation;
- pricing and overflow protection;
- inventory counting and consumption;
- transaction ordering and rollback;
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

## Packaging and license boundary

The universal JAR contains both thin adapters but does not contain FTB Chunks or OpenPAC classes/assets.

- FTB Chunks is an external All Rights Reserved / visible-source dependency.
- OpenPAC is an external LGPL-3.0-only dependency.
- BuyClaimChunks Continued remains MIT-licensed.
- Both APIs are compile-time-only dependencies.
- CI installs full backends only in isolated runtime configurations.
- `LICENSE`, `NOTICE`, and `THIRD_PARTY_NOTICES.md` must be packaged.
- NeoForge metadata declares both claim mods as optional alternatives.

## Test and CI gates

### Universal artifact

- exactly one non-sources JAR;
- universal file name;
- both adapter classes and unavailable adapter present;
- no FTB/OpenPAC dependency classes embedded;
- both dependencies declared optional;
- required license and notice files present.

### FTB environment

- unit/shared GameTests;
- selected backend is `ftb`;
- real purchase `0 -> 1`;
- payment `4 -> 0`;
- normal shutdown and second-JVM reload;
- dedicated server reaches `Done`.

### OpenPAC environment

- unit/shared GameTests;
- selected backend is `openpac`;
- real bonus purchase `0 -> 1`;
- zero-base full limit `0 -> 1`;
- payment `4 -> 0`;
- normal shutdown and second-JVM reload;
- dedicated server reaches `Done`.

### Misconfiguration guards

- no backend: server reaches `Done`, unavailable adapter selected;
- both backends: server reaches `Done`, unavailable adapter selected;
- neither case may silently choose a backend.

## Documentation and publication

Required release material:

- English and Japanese READMEs;
- exact defaults and setting-change steps;
- OpenPAC zero-base setup and migration guides;
- universal-JAR license review;
- bilingual Modrinth project description;
- Modrinth 1.2.0 changelog and metadata template;
- guarded upload script requiring a validated SHA-256;
- one-JAR GitHub Release workflow.

OpenPAC can be linked as an optional Modrinth dependency. FTB Chunks remains an external official FTB/CurseForge installation when no official Modrinth project can be resolved.

## Definition of done

The work is complete when one universal artifact exposes identical commands, settings, prices, limits, payment safety, and restart persistence with either supported backend; safely disables purchases with both or neither backend; passes every self-hosted CI gate; and is ready for explicit merge and release authorization.

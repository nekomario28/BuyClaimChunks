# OpenPAC backend feature-parity plan

## Status and baseline

- Repository: `nekomario28/BuyClaimChunks`
- Baseline branch: `main`
- Baseline commit: `22d7adcbe5f711a3bc7e2cb8593c60e19838dce1`
- Current release line: BuyClaimChunks Continued `1.1.1`
- Target: Minecraft `1.21.1`, NeoForge `21.1.x`, Java `21`

## Goal

Add an Open Parties and Claims (OpenPAC) build whose player-visible behavior is the same as the existing FTB Chunks build.

The OpenPAC build must sell personal OpenPAC bonus claim capacity. The intended server model is:

```text
base claim limit = 0
party/member free claim bonuses = 0
purchased capacity = OpenPAC BONUS_CHUNK_CLAIMS
full claim limit = purchased capacity
```

The mod must not silently edit OpenPAC's server configuration. It must document the required zero-base configuration and emit a clear warning when the effective base limit is non-zero or the bonus option cannot be changed.

## Feature-parity contract

Both backend builds must provide the same behavior:

- `/buyclaim` buys one personal extra-claim slot.
- `/buyclaim <amount>` buys multiple slots in one transaction.
- The command remains player-only.
- The same config file name and keys are used:
  - `itemRequired`
  - `amountRequired`
  - `priceGrowthFactor`
  - `priceExponent`
  - `maxExtraClaims`
  - `maxPurchaseAmount`
- The same defaults and progressive price formula are used.
- Current backend-owned extra capacity determines the next price.
- Administrator-granted extra capacity affects price and the configured maximum.
- Payment is counted across the normal 36-slot inventory, including the hotbar, but excluding armor and offhand.
- Payment is consumed only after the backend confirms the capacity update.
- Failed capacity updates consume no items.
- Batch pricing and overflow behavior remain identical.
- No independent purchase-count database is introduced.
- A successful purchase survives normal server shutdown and restart.
- The mod does not automatically claim a chunk, sell force-loaded chunks, change party allowances, charge upkeep, refund unclaims, or transfer capacity.

## Release structure

Produce two mutually exclusive JARs from shared source:

```text
buyclaimchunks-continued-ftb-neoforge-1.21.1-1.2.0.jar
buyclaimchunks-continued-openpac-neoforge-1.21.1-1.2.0.jar
```

Both retain mod ID `buyclaimchunks` and the existing config path for compatibility. They must not be installed together.

A shared implementation is required so that command behavior, messages, pricing, payment, validation, and configuration cannot drift between backends.

## Architecture

### Shared purchase core

Extract backend-independent behavior into shared classes:

- command registration and argument validation
- configuration
- pricing calculation
- inventory counting and consumption
- purchase orchestration
- player messages and structured logging

Introduce a narrow backend contract similar to:

```java
interface ClaimCapacityBackend {
    String id();
    int getExtraClaims(ServerPlayer player);
    GrantResult setExtraClaims(ServerPlayer player, int expectedCurrent, int newValue);
    int getFullClaimLimit(ServerPlayer player);
}
```

`setExtraClaims` must use compare-before-write semantics: if the current value changed after price calculation, the purchase is rejected and recalculated rather than overwriting a concurrent administrator or command update.

### FTB backend

Preserve the current FTB Chunks semantics:

- read the personal extra-claim total from FTB Chunks
- update the personal extra-claim total
- verify the new value before consuming payment
- preserve existing FTB player data and configuration behavior

Refactoring the FTB adapter must not change successful purchase prices, limits, command syntax, messages, or persistence.

### OpenPAC backend

Use the non-deprecated v2 server API:

```text
OpenPACServerAPI.get(server)
  -> getPlayerConfigManager()
  -> getLoadedConfig(player UUID)
  -> getEffective(PlayerConfigOptions.BONUS_CHUNK_CLAIMS)
  -> tryToSet(PlayerConfigOptions.BONUS_CHUNK_CLAIMS, new value)
```

Verify success by re-reading `BONUS_CHUNK_CLAIMS`. Also verify the full claim limit through `IServerClaimsManagerAPI.getPlayerFullClaimLimit(player)`.

Reject the purchase without charging when:

- OpenPAC is unavailable or not initialized
- the bonus option is not allowed for the player config
- the option is forced to a server default and `tryToSet` rejects it
- the result is not `SUCCESS`
- the re-read bonus value does not equal the requested value
- the full limit does not increase consistently

## Transaction ordering

For both backends:

1. Read the current extra capacity.
2. Check `maxPurchaseAmount` and `maxExtraClaims` using overflow-safe arithmetic.
3. Calculate the complete batch price.
4. Resolve the configured item and count inventory payment.
5. Re-read the current extra capacity to detect concurrent changes.
6. Set and verify the new extra capacity.
7. Consume payment.
8. If the already-validated payment unexpectedly cannot be consumed, attempt to restore the previous capacity, log a high-severity error, and notify the player to contact an administrator.
9. Send the normal success message only after capacity and payment are both confirmed.

## OpenPAC server configuration contract

Document the exact OpenPAC settings required to make every claim slot purchasable:

- effective base claim limit: `0`
- party/member claim bonuses: `0`
- permission/rank claim-limit override: disabled or `0`
- `BONUS_CHUNK_CLAIMS`: allowed per player and not forced to a default value

The startup/runtime diagnostic must warn, but not mutate configuration, when:

- a player has a non-zero effective base claim limit
- the effective full limit is not `base + bonus`
- the bonus option is not writable

A non-zero base limit does not prevent the mod from operating, but it means some free capacity exists and therefore does not satisfy the intended all-paid model.

## Test and CI gates

### Shared tests

- existing pricing examples and overflow cases
- batch purchases
- constant pricing
- inventory payment across multiple stacks
- insufficient payment leaves inventory unchanged
- maximum purchase and maximum total checks
- concurrent capacity-change rejection

### FTB integration

Retain and adapt the current gates:

- command registration GameTest
- real purchase `0 -> 1`
- payment `4 -> 0`
- normal shutdown persistence
- second-JVM reload of the FTB personal extra-claim total
- clean dedicated-server startup

### OpenPAC integration

Add equivalent gates:

- command registration with only OpenPAC installed
- reset bonus capacity to `0`
- real `/buyclaim` purchase `0 -> 1`
- payment `4 -> 0`
- full claim limit reflects the bonus when base is `0`
- normal shutdown persistence
- second-JVM reload of `BONUS_CHUNK_CLAIMS = 1`
- purchase rejection and zero payment when `tryToSet` is unavailable or rejected
- clean dedicated-server startup with OpenPAC and without FTB Chunks

### Packaging

For each JAR:

- exactly one backend implementation is packaged
- only the matching claim mod is declared as required
- no classes from the other backend are present
- LICENSE, NOTICE, metadata, logo, shared runtime classes, and test resources are present
- release validation publishes JAR and SHA-256 checksum

## Documentation and migration

Update English and Japanese documentation with:

- separate FTB and OpenPAC downloads
- mutually exclusive installation warning
- identical command/config/pricing behavior
- exact OpenPAC zero-base configuration
- explanation that OpenPAC bonus claims are the source of truth
- troubleshooting for a forced/non-writable bonus option

Existing FTB users must retain their FTB quota and config unchanged.

The first OpenPAC release does not automatically migrate an FTB Chunks quota into OpenPAC. Automatic cross-mod quota migration is out of scope because it can duplicate capacity when both data sets remain in the world. Administrators may grant the equivalent OpenPAC bonus manually before removing FTB Chunks.

## Phases

1. **Parity lock:** turn the existing FTB behavior into explicit shared acceptance tests.
2. **Core extraction:** introduce the backend contract and shared purchase service while keeping the FTB artifact behavior unchanged.
3. **Build split:** generate separate FTB and OpenPAC artifacts from shared source.
4. **OpenPAC adapter:** implement v2 API reads, compare-before-write, update verification, and diagnostics.
5. **Persistence evidence:** add OpenPAC two-process restart integration matching the FTB test.
6. **Packaging and docs:** verify both JARs, update bilingual documentation and release workflow.
7. **Release candidate:** run all unit, GameTest, restart, and dedicated-server gates; publish `1.2.0` only after both backend matrices pass.

## Definition of done

The work is complete only when the two artifacts expose the same command, configuration, price results, limits, inventory behavior, failure safety, and restart persistence, and when the OpenPAC artifact works on a clean Minecraft 1.21.1 NeoForge server with OpenPAC installed and FTB Chunks absent.

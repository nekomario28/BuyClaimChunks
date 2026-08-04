# Changelog

## 1.2.0 - Unreleased

- Added Open Parties and Claims support for Minecraft 1.21.1 / NeoForge.
- Replaced backend-specific artifacts with one universal JAR that automatically selects FTB Chunks or OpenPAC when exactly one is installed.
- Added fail-closed behavior: when both or neither backend is installed, the server starts but `/buyclaim` is disabled without consuming payment.
- Kept the existing mod ID, command, configuration keys, default price curve, limits, messages, and inventory behavior for both backends.
- Extracted command handling, pricing, inventory payment, limit checks, logging, and transaction ordering into a shared purchase core.
- Added compare-before-write checks so concurrent administrator changes are never overwritten by a stale purchase.
- Added verified rollback when an already validated payment unexpectedly cannot be consumed after a capacity grant.
- Stored OpenPAC purchases in `BONUS_CHUNK_CLAIMS`; no separate purchase database is introduced.
- Added diagnostics for non-zero OpenPAC base claim capacity and non-writable bonus settings without mutating OpenPAC configuration.
- Added one-JAR inspection, FTB and OpenPAC GameTests, two-process restart persistence tests, dedicated-server smoke tests, and both/neither-backend startup guards.
- Added English and Japanese default-configuration, price-curve, setting-change, OpenPAC setup, migration, and Modrinth publication documentation.
- Added third-party license notices while keeping FTB Chunks and OpenPAC external and unbundled.

### Default economy

```toml
[general]
itemRequired = "minecraft:diamond"
amountRequired = 4
priceGrowthFactor = 3.45
priceExponent = 0.5
maxExtraClaims = 100
maxPurchaseAmount = 100
```

Existing `buyclaimchunks-common.toml` files are preserved. Stop the server,
edit the file, save it, and restart to change the economy. Older configs may
retain `amountRequired = 1`; change it to `4` or regenerate the file while the
server is stopped to use the current default curve.

### OpenPAC server model

For an all-paid claim model, configure the effective OpenPAC base claim limit,
party/member bonuses, owner bonus, and permission/rank overrides to `0`.
Purchased capacity is stored in the player's effective
`BONUS_CHUNK_CLAIMS` value.

### Upgrade notes

- Existing FTB Chunks users keep their FTB personal extra-claim quota and existing BuyClaimChunks configuration.
- Replace an older BuyClaimChunks Continued JAR with the single universal 1.2.0 JAR.
- Install exactly one backend: FTB Chunks or OpenPAC.
- Automatic FTB-to-OpenPAC quota migration is not provided, to avoid duplicating capacity while both data sets remain in backups.

## 1.1.1 - 2026-07-26

- Normalized the public maintainer metadata to `nekomario28`.
- Narrowed player-command exception handling to the expected command exception type.
- Added server log warnings when FTB Chunks rejects an internal extra-claim update.
- Centralized payment counting and consumption so inventory behavior can be tested independently.
- Added exact regression tests for the published 31-item and 34-item batch-price examples.
- Added NeoForge GameTests for command registration, multi-stack payments, insufficient-payment safety, and the default price total.
- Added packaged-JAR validation for metadata, licensing notices, classes, branding, and the GameTest structure.
- Added a validated tag-triggered GitHub Release workflow that publishes the JAR and its SHA-256 checksum.
- Added an end-to-end, two-process NeoForge GameTest that executes the real `/buyclaim` command, verifies FTB Chunks personal quota `0 -> 1`, verifies payment `4 -> 0`, shuts the server down normally, restarts from the same world, and confirms the personal quota reloads as `1`.
- Added the restart-persistence check to pull-request CI and release validation.

## 1.1.0 - 2026-07-23

- Added NeoForge support for Minecraft 1.21.1.
- Added configurable progressive pricing based on each player's personal extra-claim count.
- Added safe transaction handling: items are consumed only after FTB Chunks confirms the quota update.
- Added a configurable per-command purchase limit.
- Added overflow-safe price calculation and automated pricing tests.
- Added clear MIT licensing, fork attribution, documentation, and independent branding.

### Configuration migration

Existing `buyclaimchunks-common.toml` files are not overwritten. If an older
configuration contains `amountRequired = 1`, change it to `4` or delete the
file while the server is stopped to generate the new defaults.

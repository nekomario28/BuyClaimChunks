# Changelog

## 1.2.0 - Unreleased

- Added a separate Open Parties and Claims build for Minecraft 1.21.1 / NeoForge.
- Split release artifacts into mutually exclusive FTB Chunks and OpenPAC JARs while retaining the `buyclaimchunks` mod ID and the existing configuration keys.
- Extracted the command, pricing, inventory payment, limit checks, messages, logging, and transaction ordering into a shared purchase core.
- Added backend compare-before-write checks so concurrent administrator changes are never overwritten by a stale purchase.
- Added verified rollback when the already-validated payment unexpectedly cannot be consumed after a capacity grant.
- Stored OpenPAC purchases in `BONUS_CHUNK_CLAIMS`; no separate purchase database is introduced.
- Added diagnostics for non-zero OpenPAC base claim capacity and non-writable bonus settings without mutating OpenPAC configuration.
- Added backend-specific JAR inspection, GameTests, two-process restart persistence tests, and dedicated-server smoke tests.
- Preserved the existing FTB behavior and validated its real `/buyclaim` purchase, payment `4 -> 0`, normal shutdown save, second-JVM quota reload, and clean dedicated-server startup on the `buyclaimchunks` self-hosted runner.

### OpenPAC server model

For an all-paid claim model, configure the effective OpenPAC base claim limit,
party/member bonuses, owner bonuses, and permission/rank overrides to `0`.
Purchased capacity is then exactly the player's effective
`BONUS_CHUNK_CLAIMS` value.

### Upgrade notes

- Existing FTB Chunks users keep their FTB personal extra-claim quota and existing `buyclaimchunks-common.toml` values.
- Do not install the FTB and OpenPAC JARs together because both intentionally use mod ID `buyclaimchunks`.
- Automatic FTB-to-OpenPAC quota migration is not provided, to avoid duplicating capacity while both claim data sets remain in a world backup.

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

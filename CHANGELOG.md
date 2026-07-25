# Changelog

## 1.1.1 - 2026-07-26

- Normalized the public maintainer metadata to `nekomario28`.
- Narrowed player-command exception handling to the expected command exception type.
- Added server log warnings when FTB Chunks rejects an internal extra-claim update.
- Centralized payment counting and consumption so inventory behavior can be tested independently.
- Added exact regression tests for the published 31-item and 34-item batch-price examples.
- Added NeoForge GameTests for command registration, multi-stack payments, insufficient-payment safety, and the default price total.
- Added packaged-JAR validation for metadata, licensing notices, classes, branding, and the GameTest structure.
- Added a validated tag-triggered GitHub Release workflow that publishes the JAR and its SHA-256 checksum.

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

# Changelog

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

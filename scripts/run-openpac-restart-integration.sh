#!/usr/bin/env bash
set -euo pipefail

rm -rf run-integration-openpac
rm -f openpac-integration-seed.log openpac-integration-verify.log

# NeoForge copies server configs from defaultconfigs into a newly created world.
# Keep the integration world on the intended all-paid model: no free base,
# party, owner, or permission-derived claim capacity.
mkdir -p run-integration-openpac/defaultconfigs
cat > run-integration-openpac/defaultconfigs/openpartiesandclaims-server.toml <<'EOF'
[serverConfig]
permissionSystem = ""

[serverConfig.claims]
enabled = true
partyOwnedClaims = false
maxPlayerClaims = 0
maxPlayerClaimsPermission = ""
claimBonusPerPartyMember = 0
claimBonusForPartyOwner = 0
EOF

set -o pipefail
./gradlew runIntegrationSeed \
  -Pclaim_backend=openpac \
  --no-daemon --console=plain 2>&1 | tee openpac-integration-seed.log

test -s run-integration-openpac/purchase-state.properties

./gradlew runIntegrationVerify \
  -Pclaim_backend=openpac \
  --no-daemon --console=plain 2>&1 | tee openpac-integration-verify.log

grep -Eq 'All [0-9]+ required tests passed' openpac-integration-seed.log
grep -Eq 'All [0-9]+ required tests passed' openpac-integration-verify.log

echo 'OpenPAC bonus purchase, zero-base full limit, payment, shutdown save, and restart reload integration passed.'

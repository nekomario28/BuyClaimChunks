#!/usr/bin/env bash
set -euo pipefail

rm -rf run-openpac-party
rm -f openpac-party-semantics.log

# Dedicated world for verifying OpenPAC's actual party-owned claim model.
# Use the built-in "default" party system explicitly. OpenPAC's stock config
# prefers "ftb_teams" and falls back to default when it is unavailable, but the
# semantics probe must not depend on that fallback. All free capacity is zero so
# every successful claim is backed by per-player BONUS_CHUNK_CLAIMS purchased
# through /buyclaim.
mkdir -p run-openpac-party/defaultconfigs
cat > run-openpac-party/defaultconfigs/openpartiesandclaims-server.toml <<'EOF'
[serverConfig]
permissionSystem = ""
primaryPartySystem = "default"

[serverConfig.claims]
enabled = true
partyOwnedClaims = true
maxPlayerClaims = 0
maxPlayerClaimsPermission = ""
claimBonusPerPartyMember = 0
claimBonusForPartyOwner = 0
EOF

set -o pipefail
./gradlew runOpenPacPartySemantics \
  -Ptest_backend=openpac \
  --no-daemon --console=plain 2>&1 | tee openpac-party-semantics.log

grep -Eq 'All [0-9]+ required tests passed' openpac-party-semantics.log
grep -Fq 'Configured OPAC to use the following party system as primary: default' openpac-party-semantics.log
grep -Fq 'OpenPAC party semantics verified:' openpac-party-semantics.log
grep -Fq 'initialized with openpac backend' openpac-party-semantics.log

echo 'OpenPAC party semantics passed: UUID-bound personal ledgers, owner-shared PARTY claims, leave behavior, and owner-transfer behavior are verified.'

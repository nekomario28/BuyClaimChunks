#!/usr/bin/env bash
set -euo pipefail

rm -rf run-integration
rm -f integration-seed.log integration-verify.log

set -o pipefail
./gradlew runIntegrationSeed --no-daemon --console=plain | tee integration-seed.log

test -s run-integration/purchase-state.properties

./gradlew runIntegrationVerify --no-daemon --console=plain | tee integration-verify.log

grep -Fq 'All required tests passed' integration-seed.log
grep -Fq 'All required tests passed' integration-verify.log

echo 'FTB Chunks purchase, payment, shutdown save, and restart reload integration passed.'

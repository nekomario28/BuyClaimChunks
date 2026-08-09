#!/usr/bin/env bash
set -euo pipefail

rm -rf run-integration-ftb
rm -f integration-seed.log integration-verify.log

set -o pipefail
./gradlew runIntegrationSeed \
  -Ptest_backend=ftb \
  --no-daemon --console=plain 2>&1 | tee integration-seed.log

test -s run-integration-ftb/purchase-state.properties

./gradlew runIntegrationVerify \
  -Ptest_backend=ftb \
  --no-daemon --console=plain 2>&1 | tee integration-verify.log

grep -Eq 'All [0-9]+ required tests passed' integration-seed.log
grep -Eq 'All [0-9]+ required tests passed' integration-verify.log
grep -Fq 'initialized with ftb backend' integration-seed.log

echo 'Universal JAR: FTB Chunks purchase, payment, shutdown save, and restart reload passed.'

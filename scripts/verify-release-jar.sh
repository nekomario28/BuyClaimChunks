#!/usr/bin/env bash
set -euo pipefail

shopt -s nullglob
all_jars=(build/libs/*.jar)
release_jars=()

for candidate in "${all_jars[@]}"; do
  case "$(basename "$candidate")" in
    *-sources.jar|*-javadoc.jar)
      ;;
    *)
      release_jars+=("$candidate")
      ;;
  esac
done

if [[ ${#release_jars[@]} -ne 1 ]]; then
  printf 'Expected exactly one universal release JAR in build/libs, found %d.\n' "${#release_jars[@]}" >&2
  printf 'Candidates: %s\n' "${release_jars[*]:-none}" >&2
  exit 1
fi

jar_file=${release_jars[0]}
if [[ $(basename "$jar_file") != buyclaimchunks-continued-neoforge-* ]]; then
  printf 'Unexpected universal release JAR name: %s\n' "$jar_file" >&2
  exit 1
fi

entries=$(jar tf "$jar_file")
required_entries=(
  META-INF/neoforge.mods.toml
  buyclaimchunks_continued.png
  LICENSE
  NOTICE
  THIRD_PARTY_NOTICES.md
  me/skyadri/buyclaimchunks/BuyClaimChunks.class
  me/skyadri/buyclaimchunks/BuyClaimCommand.class
  me/skyadri/buyclaimchunks/BuyClaimChunksGameTests.class
  me/skyadri/buyclaimchunks/BuyClaimChunksRestartIntegrationGameTests.class
  me/skyadri/buyclaimchunks/ClaimCapacityBackend.class
  me/skyadri/buyclaimchunks/ClaimCapacityUpdate.class
  me/skyadri/buyclaimchunks/ClaimCapacityBackends.class
  me/skyadri/buyclaimchunks/UnavailableClaimCapacityBackend.class
  me/skyadri/buyclaimchunks/FtbClaimCapacityBackend.class
  me/skyadri/buyclaimchunks/OpenPacClaimCapacityBackend.class
  data/buyclaimchunks/structure/empty.nbt
)

for required in "${required_entries[@]}"; do
  if ! grep -Fxq "$required" <<<"$entries"; then
    printf 'Universal release JAR is missing required entry: %s\n' "$required" >&2
    exit 1
  fi
done

if grep -Fq 'BuyClaimChunksOpenPacRestartIntegrationGameTests.class' <<<"$entries"; then
  printf 'Universal release JAR contains the removed backend-specific OpenPAC integration test.\n' >&2
  exit 1
fi

metadata=$(unzip -p "$jar_file" META-INF/neoforge.mods.toml)
manifest=$(unzip -p "$jar_file" META-INF/MANIFEST.MF)

if ! grep -Fq 'BuyClaimChunks-Backends: ftbchunks,openpartiesandclaims' <<<"$manifest"; then
  printf 'Universal release JAR manifest does not identify both supported backends.\n' >&2
  exit 1
fi

for backend_mod in ftbchunks openpartiesandclaims; do
  grep -Fq "modId=\"${backend_mod}\"" <<<"$metadata" || {
    printf 'Universal metadata does not mention optional backend %s.\n' "$backend_mod" >&2
    exit 1
  }
done

optional_count=$(grep -c 'type="optional"' <<<"$metadata")
if [[ "$optional_count" -ne 2 ]]; then
  printf 'Expected exactly two optional backend dependencies, found %s.\n' "$optional_count" >&2
  exit 1
fi

if grep -A5 'modId="ftbchunks"' <<<"$metadata" | grep -Fq 'type="required"'; then
  printf 'FTB Chunks must not be mandatory in the universal metadata.\n' >&2
  exit 1
fi
if grep -A5 'modId="openpartiesandclaims"' <<<"$metadata" | grep -Fq 'type="required"'; then
  printf 'OpenPAC must not be mandatory in the universal metadata.\n' >&2
  exit 1
fi

mod_version=$(sed -n 's/^mod_version=//p' gradle.properties)
mod_authors=$(sed -n 's/^mod_authors=//p' gradle.properties)
if ! grep -Fq "version=\"${mod_version}\"" <<<"$metadata"; then
  printf 'Release JAR metadata does not contain version %s.\n' "$mod_version" >&2
  exit 1
fi
if ! grep -Fq "authors=\"${mod_authors}\"" <<<"$metadata"; then
  printf 'Release JAR metadata does not contain authors %s.\n' "$mod_authors" >&2
  exit 1
fi

printf 'Verified universal FTB Chunks/OpenPAC release JAR: %s\n' "$jar_file" >&2
printf '%s\n' "$jar_file"

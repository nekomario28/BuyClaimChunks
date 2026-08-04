#!/usr/bin/env bash
set -euo pipefail

backend=${1:-${CLAIM_BACKEND:-ftb}}
if [[ "$backend" != "ftb" && "$backend" != "openpac" ]]; then
  printf 'Unsupported backend: %s\n' "$backend" >&2
  exit 1
fi

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
  printf 'Expected exactly one release JAR in build/libs, found %d.\n' "${#release_jars[@]}" >&2
  printf 'Candidates: %s\n' "${release_jars[*]:-none}" >&2
  exit 1
fi

jar_file=${release_jars[0]}
if [[ $(basename "$jar_file") != *"-${backend}-neoforge-"* ]]; then
  printf 'Release JAR name does not identify backend %s: %s\n' "$backend" "$jar_file" >&2
  exit 1
fi

entries=$(jar tf "$jar_file")
required_entries=(
  META-INF/neoforge.mods.toml
  buyclaimchunks_continued.png
  LICENSE
  NOTICE
  me/skyadri/buyclaimchunks/BuyClaimChunks.class
  me/skyadri/buyclaimchunks/BuyClaimCommand.class
  me/skyadri/buyclaimchunks/BuyClaimChunksGameTests.class
  me/skyadri/buyclaimchunks/ClaimCapacityBackend.class
  me/skyadri/buyclaimchunks/ClaimCapacityUpdate.class
  me/skyadri/buyclaimchunks/ClaimCapacityBackends.class
  data/buyclaimchunks/structure/empty.nbt
)

for required in "${required_entries[@]}"; do
  if ! grep -Fxq "$required" <<<"$entries"; then
    printf 'Release JAR is missing required entry: %s\n' "$required" >&2
    exit 1
  fi
done

metadata=$(unzip -p "$jar_file" META-INF/neoforge.mods.toml)
manifest=$(unzip -p "$jar_file" META-INF/MANIFEST.MF)

if ! grep -Fq "BuyClaimChunks-Backend: ${backend}" <<<"$manifest"; then
  printf 'Release JAR manifest does not identify backend %s.\n' "$backend" >&2
  exit 1
fi

if [[ "$backend" == "ftb" ]]; then
  grep -Fxq 'me/skyadri/buyclaimchunks/FtbClaimCapacityBackend.class' <<<"$entries" || {
    printf 'FTB release JAR is missing FtbClaimCapacityBackend.\n' >&2
    exit 1
  }
  grep -Fxq 'me/skyadri/buyclaimchunks/BuyClaimChunksRestartIntegrationGameTests.class' <<<"$entries" || {
    printf 'FTB release JAR is missing restart integration coverage.\n' >&2
    exit 1
  }
  if grep -Fq 'OpenPacClaimCapacityBackend.class' <<<"$entries"; then
    printf 'FTB release JAR unexpectedly contains the OpenPAC backend.\n' >&2
    exit 1
  fi
  grep -Fq 'modId="ftbchunks"' <<<"$metadata" || {
    printf 'FTB release metadata does not require FTB Chunks.\n' >&2
    exit 1
  }
else
  grep -Fxq 'me/skyadri/buyclaimchunks/OpenPacClaimCapacityBackend.class' <<<"$entries" || {
    printf 'OpenPAC release JAR is missing OpenPacClaimCapacityBackend.\n' >&2
    exit 1
  }
  if grep -Fq 'FtbClaimCapacityBackend.class' <<<"$entries"; then
    printf 'OpenPAC release JAR unexpectedly contains the FTB backend.\n' >&2
    exit 1
  fi
  if grep -Fq 'BuyClaimChunksRestartIntegrationGameTests.class' <<<"$entries"; then
    printf 'OpenPAC release JAR unexpectedly contains the FTB restart integration class.\n' >&2
    exit 1
  fi
  grep -Fq 'modId="openpartiesandclaims"' <<<"$metadata" || {
    printf 'OpenPAC release metadata does not require Open Parties and Claims.\n' >&2
    exit 1
  }
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

printf 'Verified %s backend release JAR: %s\n' "$backend" "$jar_file" >&2
printf '%s\n' "$jar_file"

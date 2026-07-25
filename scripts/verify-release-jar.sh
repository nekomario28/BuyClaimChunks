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
  printf 'Expected exactly one release JAR in build/libs, found %d.\n' "${#release_jars[@]}" >&2
  printf 'Candidates: %s\n' "${release_jars[*]:-none}" >&2
  exit 1
fi

jar_file=${release_jars[0]}
entries=$(jar tf "$jar_file")
required_entries=(
  META-INF/neoforge.mods.toml
  buyclaimchunks_continued.png
  LICENSE
  NOTICE
  me/skyadri/buyclaimchunks/BuyClaimChunks.class
  me/skyadri/buyclaimchunks/BuyClaimCommand.class
  me/skyadri/buyclaimchunks/BuyClaimChunksGameTests.class
  data/buyclaimchunks/structure/empty.nbt
)

for required in "${required_entries[@]}"; do
  if ! grep -Fxq "$required" <<<"$entries"; then
    printf 'Release JAR is missing required entry: %s\n' "$required" >&2
    exit 1
  fi
done

mod_version=$(sed -n 's/^mod_version=//p' gradle.properties)
mod_authors=$(sed -n 's/^mod_authors=//p' gradle.properties)
metadata=$(unzip -p "$jar_file" META-INF/neoforge.mods.toml)

if ! grep -Fq "version=\"${mod_version}\"" <<<"$metadata"; then
  printf 'Release JAR metadata does not contain version %s.\n' "$mod_version" >&2
  exit 1
fi

if ! grep -Fq "authors=\"${mod_authors}\"" <<<"$metadata"; then
  printf 'Release JAR metadata does not contain authors %s.\n' "$mod_authors" >&2
  exit 1
fi

printf 'Verified packaged release JAR: %s\n' "$jar_file" >&2
printf '%s\n' "$jar_file"

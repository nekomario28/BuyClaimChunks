# Universal JAR license review

## Decision

BuyClaimChunks Continued remains MIT-licensed and publishes one universal JAR.
The JAR contains this project's shared purchase logic and two small compatibility
adapters. It does not embed or redistribute FTB Chunks, Open Parties and Claims,
or their assets.

## FTB Chunks

FTB Chunks identifies its source as visible source and its license as All Rights
Reserved. The universal JAR therefore treats FTB Chunks only as an optional,
externally installed runtime dependency. The build uses its API artifacts for
compilation and isolated CI validation; published artifacts must not include
FTB classes, resources, or license-controlled assets.

## Open Parties and Claims

OpenPAC is LGPL-3.0-only and publishes a public API intended for integration by
other mods. The universal JAR links to the separately installed OpenPAC mod at
runtime and does not embed it. This shared-library style keeps OpenPAC separately
replaceable and preserves this project's MIT licensing. The distributed JAR
includes a prominent third-party notice and directs users to OpenPAC's source
and license.

## Packaging gates

Release verification must prove that:

- exactly one BuyClaimChunks Continued JAR is produced;
- both adapter classes are present;
- no external FTB or OpenPAC classes are packaged under their namespaces;
- FTB Chunks and OpenPAC are declared optional, not required or embedded;
- `LICENSE`, `NOTICE`, and `THIRD_PARTY_NOTICES.md` are included;
- the mod safely disables purchases when neither or both backends are installed.

## Distribution guidance

Modrinth and GitHub Releases should publish only the universal JAR and its
checksum. FTB Chunks and OpenPAC remain separate downloads. The project page
must tell users to install exactly one supported claim backend.

This review documents the technical packaging approach and is not legal advice.

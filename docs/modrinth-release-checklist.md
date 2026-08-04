# Modrinth 1.2.0 release checklist

Do not publish until PR #8 is merged and the release commit has passed the full universal-JAR self-hosted workflow.

## 1. Validate source and artifact

- [ ] PR #8 is no longer Draft and has explicit merge authorization.
- [ ] Universal-JAR CI passes FTB, OpenPAC, no-backend, and dual-backend configurations.
- [ ] The successful workflow artifact contains exactly one release JAR.
- [ ] Record the JAR SHA-256 from the successful evidence.
- [ ] Confirm the JAR name is `buyclaimchunks-continued-neoforge-1.21.1-1.2.0.jar`.
- [ ] Confirm `LICENSE`, `NOTICE`, and `THIRD_PARTY_NOTICES.md` are packaged.

## 2. Update the project page

Review:

```text
docs/modrinth-description.md
```

Then run with a token that has project-edit permission:

```fish
read --silent --prompt-str="Modrinth token: " MODRINTH_TOKEN
set -x MODRINTH_TOKEN $MODRINTH_TOKEN
fish scripts/modrinth-sync-project.fish
set -e MODRINTH_TOKEN
```

Verify on the project page:

- [ ] one universal JAR is described;
- [ ] users are told to install exactly one backend;
- [ ] default settings and change instructions are visible;
- [ ] FTB Chunks is described as an external official FTB/CurseForge download;
- [ ] OpenPAC is described as an optional dependency;
- [ ] server required / client optional and MIT metadata are correct.

## 3. Upload version 1.2.0

Extract the exact successful CI artifact without rebuilding it. Place the JAR at the default `build/libs/` path or set `MODRINTH_RELEASE_JAR` to its location.

```fish
set -x EXPECTED_SHA256 '<SHA-256 from successful CI>'
set -x MODRINTH_RELEASE_JAR '/path/to/buyclaimchunks-continued-neoforge-1.21.1-1.2.0.jar' # optional
read --silent --prompt-str="Modrinth token: " MODRINTH_TOKEN
set -x MODRINTH_TOKEN $MODRINTH_TOKEN
fish scripts/modrinth-upload-1.2.0.fish
set -e MODRINTH_TOKEN
set -e EXPECTED_SHA256
set -e MODRINTH_RELEASE_JAR
```

The upload script must reject:

- a missing token;
- a missing expected SHA-256;
- a locally rebuilt or otherwise different JAR;
- a JAR missing either adapter or license notices;
- metadata that does not declare both backend alternatives as optional.

## 4. Post-publication checks

- [ ] Version is listed as release for Minecraft 1.21.1 / NeoForge.
- [ ] Only one primary JAR is attached.
- [ ] Environment is server required / client optional.
- [ ] OpenPAC appears as optional content.
- [ ] The changelog matches `docs/modrinth-release-1.2.0.md`.
- [ ] Downloaded JAR SHA-256 matches the validated release SHA.
- [ ] No FTB Chunks or OpenPAC binary is redistributed by the project.

Never commit or paste the Modrinth token into files, issues, logs, or chat.

#!/usr/bin/env fish

set -l SCRIPT_DIR (cd (dirname (status --current-filename)); and pwd)
set -l REPO_ROOT (cd "$SCRIPT_DIR/.."; and pwd)
set -l PROJECT_SLUG "buyclaimchunks-continued"
set -l OPENPAC_PROJECT_ID "gF3BGWvG"
set -l VERSION "1.2.0"
set -l DEFAULT_JAR "$REPO_ROOT/build/libs/buyclaimchunks-continued-neoforge-1.21.1-$VERSION.jar"
set -l CHANGELOG "$REPO_ROOT/docs/modrinth-release-1.2.0.md"
set -l USER_AGENT "nekomario28/BuyClaimChunks-Continued/$VERSION"

if set -q MODRINTH_RELEASE_JAR; and test -n "$MODRINTH_RELEASE_JAR"
    set JAR "$MODRINTH_RELEASE_JAR"
else
    set JAR "$DEFAULT_JAR"
end

if not set -q MODRINTH_TOKEN; or test -z "$MODRINTH_TOKEN"
    echo "MODRINTH_TOKEN is not set." >&2
    echo 'Example: read --silent --prompt-str="Modrinth token: " MODRINTH_TOKEN; set -x MODRINTH_TOKEN $MODRINTH_TOKEN' >&2
    exit 1
end

if not set -q EXPECTED_SHA256; or test -z "$EXPECTED_SHA256"
    echo "EXPECTED_SHA256 is not set." >&2
    echo "Set it to the SHA-256 recorded for the fully validated universal CI artifact." >&2
    exit 1
end

for command_name in curl jq sha256sum mktemp unzip jar
    if not type -q $command_name
        echo "Required command is missing: $command_name" >&2
        exit 1
    end
end

cd "$REPO_ROOT"; or exit 1

for required_file in "$JAR" "$CHANGELOG"
    if not test -f "$required_file"
        echo "Missing file: $required_file" >&2
        echo "Download and extract the exact successful CI artifact; do not rebuild it before upload." >&2
        exit 1
    end
end

# Release upload must use the exact CI artifact. Rebuilding here could produce a
# different byte-for-byte JAR even from identical source, so the script only
# inspects and hashes the provided file.
set -l actual_sha256 (sha256sum "$JAR" | string split ' ' | head -n 1)
if test "$actual_sha256" != "$EXPECTED_SHA256"
    echo "JAR SHA-256 does not match the validated release file." >&2
    echo "Expected: $EXPECTED_SHA256" >&2
    echo "Actual:   $actual_sha256" >&2
    exit 1
end

set -l entries (jar tf "$JAR")
for required_entry in \
    META-INF/neoforge.mods.toml \
    LICENSE \
    NOTICE \
    THIRD_PARTY_NOTICES.md \
    me/skyadri/buyclaimchunks/FtbClaimCapacityBackend.class \
    me/skyadri/buyclaimchunks/OpenPacClaimCapacityBackend.class \
    me/skyadri/buyclaimchunks/UnavailableClaimCapacityBackend.class
    if not string match -qx "$required_entry" $entries
        echo "Release JAR is missing required universal entry: $required_entry" >&2
        exit 1
    end
end

set -l metadata (unzip -p "$JAR" META-INF/neoforge.mods.toml)
if not string match -q '*modId="ftbchunks"*type="optional"*' (string join '' $metadata)
    echo "Release metadata does not declare FTB Chunks as optional." >&2
    exit 1
end
if not string match -q '*modId="openpartiesandclaims"*type="optional"*' (string join '' $metadata)
    echo "Release metadata does not declare OpenPAC as optional." >&2
    exit 1
end

set -l project_response (mktemp)
set -l version_data (mktemp)
set -l upload_response (mktemp)
function cleanup --on-event fish_exit
    rm -f "$project_response" "$version_data" "$upload_response"
end

echo "Resolving Modrinth project ID for $PROJECT_SLUG..."
if not curl --fail-with-body --silent --show-error \
    -H "Authorization: $MODRINTH_TOKEN" \
    -H "User-Agent: $USER_AGENT" \
    "https://api.modrinth.com/v2/project/$PROJECT_SLUG" \
    > "$project_response"
    echo "Could not find or access the Modrinth project." >&2
    exit 1
end

set -l project_id (jq -r '.id // empty' "$project_response")
if test -z "$project_id"
    echo "The project response did not contain an ID." >&2
    cat "$project_response" >&2
    exit 1
end

jq -n \
    --arg project_id "$project_id" \
    --arg openpac_project_id "$OPENPAC_PROJECT_ID" \
    --rawfile changelog "$CHANGELOG" \
    '{
      name: "BuyClaimChunks Continued 1.2.0",
      version_number: "1.2.0",
      changelog: $changelog,
      dependencies: [
        {
          project_id: $openpac_project_id,
          dependency_type: "optional"
        }
      ],
      game_versions: ["1.21.1"],
      version_type: "release",
      loaders: ["neoforge"],
      featured: true,
      status: "listed",
      project_id: $project_id,
      file_parts: ["file"],
      primary_file: "file",
      environment: "server_only_client_optional"
    }' > "$version_data"

echo "Uploading exact validated universal JAR as version $VERSION..."
if not curl --fail-with-body --silent --show-error \
    -X POST "https://api.modrinth.com/v2/version" \
    -H "Authorization: $MODRINTH_TOKEN" \
    -H "User-Agent: $USER_AGENT" \
    -F "data=@$version_data;type=application/json" \
    -F "file=@$JAR;type=application/java-archive" \
    > "$upload_response"
    echo "Upload failed. Modrinth response:" >&2
    cat "$upload_response" >&2
    exit 1
end

echo "Upload succeeded:"
jq '{id, project_id, name, version_number, version_type, status, date_published, dependencies, files}' "$upload_response"

#!/usr/bin/env fish

set -l SCRIPT_DIR (cd (dirname (status --current-filename)); and pwd)
set -l REPO_ROOT (cd "$SCRIPT_DIR/.."; and pwd)
set -l PROJECT_SLUG "buyclaimchunks-continued"
set -l DESCRIPTION_FILE "$REPO_ROOT/docs/modrinth-description.md"
set -l USER_AGENT "nekomario28/BuyClaimChunks-Continued/1.2.0"

if not set -q MODRINTH_TOKEN; or test -z "$MODRINTH_TOKEN"
    echo "MODRINTH_TOKEN is not set." >&2
    exit 1
end

for command_name in curl jq mktemp
    if not type -q $command_name
        echo "Required command is missing: $command_name" >&2
        exit 1
    end
end

if not test -f "$DESCRIPTION_FILE"
    echo "Missing Modrinth description: $DESCRIPTION_FILE" >&2
    exit 1
end

set -l request_json (mktemp)
set -l response_json (mktemp)
function cleanup --on-event fish_exit
    rm -f "$request_json" "$response_json"
end

jq -n \
    --rawfile body "$DESCRIPTION_FILE" \
    '{
      title: "BuyClaimChunks Continued",
      description: "Buy personal FTB Chunks or OpenPAC claim capacity with a configurable item economy.",
      body: $body,
      client_side: "optional",
      server_side: "required",
      license_id: "MIT",
      source_url: "https://github.com/nekomario28/BuyClaimChunks",
      issues_url: "https://github.com/nekomario28/BuyClaimChunks/issues"
    }' > "$request_json"

echo "Updating Modrinth project $PROJECT_SLUG..."
if not curl --fail-with-body --silent --show-error \
    -X PATCH "https://api.modrinth.com/v2/project/$PROJECT_SLUG" \
    -H "Authorization: $MODRINTH_TOKEN" \
    -H "User-Agent: $USER_AGENT" \
    -H "Content-Type: application/json" \
    --data-binary "@$request_json" \
    > "$response_json"
    echo "Project update failed." >&2
    cat "$response_json" >&2
    exit 1
end

echo "Project description and metadata updated."

#!/bin/sh
set -eu

status_dir=${STATUS_DIR:-/tmp/dashboard-status}
mkdir -p "$status_dir/checks"

jq -n '[
    $ENV | to_entries[] | select(.key | startswith("HEALTH_")) |
    (.key | ltrimstr("HEALTH_")) as $id |
    {id: $id, healthUrl: .value,
     title: ($ENV["TITLE_" + $id] // ("eudiw-" + $id)),
     description: ($ENV["DESCRIPTION_" + $id] // ""),
     link: ($ENV["LINK_" + $id] // "")}
] | sort_by(.id) |
if all(.[]; .id | test("^[a-zA-Z0-9][a-zA-Z0-9_.-]*$")) then .
else error("Ugyldig Compose-tjenestenavn") end' > "$status_dir/targets.json"
jq -c '.[]' "$status_dir/targets.json" > "$status_dir/targets.ndjson"

jq '{updatedAt: 0, services: map(del(.healthUrl) + {ready: null})}' \
    "$status_dir/targets.json" > "$status_dir/status.json"

while :; do
    started=$(date +%s)
    while IFS= read -r service; do
        id=$(printf '%s' "$service" | jq -r '.id')
        # En intern skriptfeil må ikke gjøre forrige rundes resultat ferskt igjen.
        printf '%s' "$service" | jq 'del(.healthUrl) + {ready: null}' \
            > "$status_dir/checks/$id.json"
        (
            url=$(printf '%s' "$service" | jq -r '.healthUrl')
            ready=false
            if code=$(curl --silent --show-error --fail --noproxy '*' --max-time 2 \
                --proto '=http,https' --output /dev/null --write-out '%{http_code}' "$url"); then
                case "$code" in
                    2??) ready=true ;;
                esac
            fi
            printf '%s' "$service" | jq --argjson ready "$ready" \
                'del(.healthUrl) + {ready: $ready}' > "$status_dir/checks/$id.tmp"
            mv "$status_dir/checks/$id.tmp" "$status_dir/checks/$id.json"
        ) &
    done < "$status_dir/targets.ndjson"
    wait

    if [ -s "$status_dir/targets.ndjson" ]; then
        jq -s '{updatedAt: (now | floor), services: sort_by(.id)}' \
            "$status_dir"/checks/*.json > "$status_dir/status.tmp"
    else
        jq -n '{updatedAt: (now | floor), services: []}' > "$status_dir/status.tmp"
    fi
    mv "$status_dir/status.tmp" "$status_dir/status.json"

    if [ "${1:-}" = "--once" ]; then
        exit 0
    fi
    delay=$((5 - ($(date +%s) - started)))
    if [ "$delay" -gt 0 ]; then
        sleep "$delay"
    fi
done

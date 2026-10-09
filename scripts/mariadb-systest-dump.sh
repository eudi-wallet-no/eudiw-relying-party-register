#!/usr/bin/env bash
set +x
set -euo pipefail

if [[ $# -eq 1 && ( "$1" == "--help" || "$1" == "-h" ) ]]; then
    echo "Bruk: bash scripts/mariadb-systest-dump.sh"
    exit 0
fi

if [[ $# -ne 0 ]]; then
    echo "Bruk: bash scripts/mariadb-systest-dump.sh" >&2
    exit 1
fi

namespace="digdir-dl-digital-lommebok-systest"
expected_server="https://api.eid-systest.norwayeast.aroapp.io:6443"
port=33067

# Applikasjon | podens app.kubernetes.io/name | kildedatabase |
# Compose-prosjekt | databasetjeneste | lokal database | applikasjonstjeneste.
database_config='eudiw-rp-register-service|digital-lommebok-rp-register-mariadb|rp_register|digital-lommebok-rp|rp-register-db|rp_register_db|rp-register-service
eudiw-ca-service|digital-lommebok-ca-mariadb|eudiw_ca|digital-lommebok-rp|ca-db|ca_db|ca-service
eudiw-issuer-server|digital-lommebok-issuer-mariadb|issuer_server_db|digital-lommebok-issuer|issuer-db|issuer_server_db|issuer-server
eudiw-status-list|digital-lommebok-status-list-mariadb|status_list_db|digital-lommebok-issuer|status-list-db|status_list_db|status-list'

if ! command -v oc >/dev/null 2>&1; then
    echo "oc mangler. Installer OpenShift CLI og logg inn manuelt med oc login." >&2
    exit 1
fi

if ! command -v fzf >/dev/null 2>&1; then
    echo "fzf mangler. Installer det for å velge MariaDB-pod." >&2
    exit 1
fi

if command -v mariadb-dump >/dev/null 2>&1; then
    dump_command="mariadb-dump"
elif command -v mysqldump >/dev/null 2>&1; then
    dump_command="mysqldump"
else
    echo "Installer mariadb-dump (anbefalt) eller mysqldump." >&2
    exit 1
fi

if ! context="$(oc config current-context)"; then
    echo "Kunne ikke hente Kubernetes-kontekst. Logg inn manuelt med oc login." >&2
    exit 1
fi
if ! server="$(oc --context="$context" config view --minify \
    -o jsonpath='{.clusters[0].cluster.server}')"; then
    echo "Kunne ikke hente serveradressen for kontekst $context." >&2
    exit 1
fi
if [[ "$server" != "$expected_server" ]]; then
    echo "Feil miljø: kontekst $context peker mot $server, ikke $expected_server." >&2
    exit 1
fi

echo "Kontrollerer innlogging mot $expected_server..."
if ! oc --context="$context" --request-timeout=10s whoami </dev/null >/dev/null 2>&1; then
    echo "Kunne ikke bekrefte innlogging mot $expected_server. Innloggingen kan ha utløpt, eller API-et er utilgjengelig." >&2
    echo "Kontroller nettverk/VPN og logg inn manuelt med oc login --server=$expected_server med et nytt token." >&2
    exit 1
fi

pod_selector="app.kubernetes.io/instance=mariadb,app.kubernetes.io/component=primary,app.kubernetes.io/part-of=digital-lommebok"
pod_columns='custom-columns=NAME:.metadata.name,APP:.metadata.labels.app\.kubernetes\.io/name,READY:.status.conditions[?(@.type=="Ready")].status'
if ! pod_rows="$(oc --context="$context" --namespace="$namespace" --request-timeout=10s get pods \
    --field-selector=status.phase=Running --selector="$pod_selector" -o "$pod_columns" --no-headers </dev/null)"; then
    echo "Kunne ikke hente podene i $namespace. Kontroller innlogging og tilgang." >&2
    exit 1
fi

mariadb_pods=""
while IFS='|' read -r application pod_label database compose_project compose_database_service local_database compose_application_service; do
    matching_pods="$(printf '%s\n' "$pod_rows" | awk -v label="$pod_label" -v app="$application" \
        '$2 == label && $3 == "True" { print app "|" $1 }')"
    if [[ -n "$matching_pods" ]]; then
        mariadb_pods+="$matching_pods"$'\n'
    fi
done <<<"$database_config"
mariadb_pods="${mariadb_pods%$'\n'}"
if [[ -z "$mariadb_pods" ]]; then
    echo "Fant ingen kjørende og klare MariaDB-primary-poder for de konfigurerte databasene i $namespace ($context)." >&2
    exit 1
fi

if ! pod_choice="$(printf '%s\n' "$mariadb_pods" | FZF_DEFAULT_OPTS='' FZF_DEFAULT_OPTS_FILE='' \
    fzf --no-multi --delimiter='|' --prompt='MariaDB-pod> ' --header="$namespace ($context)")"; then
    echo "Podvalg avbrutt eller mislyktes. Ingen dump er startet." >&2
    exit 1
fi
if [[ -z "$pod_choice" ]] || ! printf '%s\n' "$mariadb_pods" | grep -Fxq -- "$pod_choice"; then
    echo "Ugyldig podvalg. Ingen dump er startet." >&2
    exit 1
fi
IFS='|' read -r application pod <<<"$pod_choice"
selected_config="$(printf '%s\n' "$database_config" | awk -F '|' -v app="$application" '$1 == app')"
IFS='|' read -r application pod_label database compose_project compose_database_service local_database compose_application_service <<<"$selected_config"
if [[ -z "$compose_project" || -z "$compose_database_service" || \
    ! "$database" =~ ^[a-zA-Z0-9_]+$ || ! "$local_database" =~ ^[a-zA-Z0-9_]+$ || \
    "$selected_config" == *$'\n'* ]]; then
    echo "Ugyldig databasekonfigurasjon for $application. Ingen dump er startet." >&2
    exit 1
fi

umask 077
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
forward_pid=""
forward_log=""
dump_file=""
local_service_stopped=false
local_import_started=false

stop_port_forward() {
    if [[ -n "$forward_pid" ]]; then
        if kill -0 "$forward_pid" 2>/dev/null; then
            kill "$forward_pid" 2>/dev/null || true
        fi
        wait "$forward_pid" 2>/dev/null || true
        forward_pid=""
    fi
    if [[ -n "$forward_log" ]]; then
        rm -f "$forward_log"
        forward_log=""
    fi
}

cleanup() {
    local exit_status=$?
    if [[ "$local_import_started" == true && ( "$exit_status" -eq 130 || "$exit_status" -eq 143 ) ]]; then
        echo "Import til $local_database avbrutt og kan være delvis gjennomført. Dumpen er beholdt: $output_file" >&2
    fi
    if [[ "$exit_status" -ne 0 && "$local_service_stopped" == true ]]; then
        echo "$compose_application_service ($service_container) forblir stoppet." >&2
    fi
    stop_port_forward
    if [[ -n "$dump_file" ]]; then
        rm -f "$dump_file"
    fi
}

trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

forward_log="$(mktemp)"
oc --context="$context" port-forward --namespace="$namespace" --address=127.0.0.1 \
    "$pod" "$port:3306" >"$forward_log" 2>&1 &
forward_pid=$!

ready=false
for ((attempt = 0; attempt < 30; attempt++)); do
    if ! kill -0 "$forward_pid" 2>/dev/null; then
        break
    fi
    if grep -q "Forwarding from 127.0.0.1:$port -> 3306" "$forward_log"; then
        ready=true
        break
    fi
    sleep 1
done

if [[ "$ready" != true ]]; then
    echo "Port-forward mot $pod i $namespace kunne ikke startes innen 30 sekunder (lokal port $port). Kontroller innlogging, nettverk/VPN og om porten er ledig." >&2
    cat "$forward_log" >&2
    exit 1
fi

mkdir -p "$script_dir/dump"
dump_file="$(mktemp "$script_dir/dump/$pod-$(date +%Y%m%d-%H%M%S)-XXXXXX")"

echo "Laster ned $database fra $pod."
echo "Oppgi root-passordet i klientens skjulte prompt."
if ! "$dump_command" --no-defaults --host=127.0.0.1 --port="$port" --protocol=TCP \
    --user=root --password \
    --single-transaction --quick \
    --routines --events --triggers "$database" >"$dump_file"; then
    echo "Dump mislyktes. Den ufullstendige filen slettes." >&2
    cat "$forward_log" >&2
    exit 1
fi

output_file="$dump_file.sql"
mv "$dump_file" "$output_file"
dump_file=""
echo "Dump lagret: $output_file"

stop_port_forward

if ! import_choice="$(printf '%s\n' 'Importer til lokal Docker' 'Lagre dump' \
    | FZF_DEFAULT_OPTS='' FZF_DEFAULT_OPTS_FILE='' fzf --no-multi --no-sort \
        --prompt='Lokal import> ' --header="Import SLETTER alle data i $local_database ($compose_project/$compose_database_service). Stopp tilkoblede apper utenfor Docker først.")"; then
    echo "Lokal import avbrutt. Dumpen er beholdt."
    exit 0
fi
case "$import_choice" in
    'Lagre dump') exit 0 ;;
    'Importer til lokal Docker') ;;
    *)
        echo "Ugyldig importvalg. Dumpen er beholdt." >&2
        exit 1
        ;;
esac

if ! command -v docker >/dev/null 2>&1; then
    echo "Docker mangler. Dumpen er beholdt." >&2
    exit 1
fi
if ! docker_context="$(docker context show)" || \
    ! docker_endpoint="$(docker context inspect "$docker_context" --format '{{.Endpoints.docker.Host}}')"; then
    echo "Kunne ikke hente Docker-kontekst og endepunkt. Ingen import er startet; dumpen er beholdt." >&2
    exit 1
fi
if [[ "$docker_endpoint" != unix://* || -n "${DOCKER_HOST:-}" ]]; then
    echo "Import krever lokal Docker-kontekst uten DOCKER_HOST. Kontekst $docker_context peker mot $docker_endpoint. Dumpen er beholdt." >&2
    exit 1
fi
docker_command=(docker --context="$docker_context")

if ! database_container="$("${docker_command[@]}" ps --quiet \
    --filter label=com.docker.compose.project="$compose_project" \
    --filter label=com.docker.compose.service="$compose_database_service")" || \
    ! service_container="$("${docker_command[@]}" ps --all --quiet \
    --filter label=com.docker.compose.project="$compose_project" \
    --filter label=com.docker.compose.service="$compose_application_service")"; then
    echo "Kunne ikke hente containere i $compose_project ($docker_context). Ingen import er startet; dumpen er beholdt." >&2
    exit 1
fi
if [[ -z "$database_container" || "$database_container" == *$'\n'* ]]; then
    echo "Import krever én kjørende databasecontainer ($compose_database_service i $compose_project). Dumpen er beholdt." >&2
    exit 1
fi
if [[ "$service_container" == *$'\n'* ]]; then
    echo "Fant flere applikasjonscontainere ($compose_application_service i $compose_project). Dumpen er beholdt." >&2
    exit 1
fi

service_was_running=false
if [[ -n "$service_container" ]]; then
    if ! service_was_running="$("${docker_command[@]}" inspect --format '{{.State.Running}}' "$service_container")"; then
        echo "Kunne ikke kontrollere kjørestatus for $compose_application_service. Ingen import er startet; dumpen er beholdt." >&2
        exit 1
    fi
    if [[ "$service_was_running" != true && "$service_was_running" != false ]]; then
        echo "Ukjent kjørestatus for $compose_application_service. Ingen import er startet; dumpen er beholdt." >&2
        exit 1
    fi
fi

if ! "${docker_command[@]}" exec "$database_container" sh -eu -c \
    'test "$MARIADB_DATABASE" = "$1"
     test -n "$MARIADB_ROOT_PASSWORD"
     MYSQL_PWD="$MARIADB_ROOT_PASSWORD" exec mariadb --no-defaults --connect-timeout=10 -uroot "$1" -e "SELECT 1"' \
    sh "$local_database" >/dev/null; then
    echo "Kunne ikke bekrefte lokal databaseinnlogging mot $local_database i $compose_database_service. Kontroller at containeren kjører og at root-passordet stemmer. Ingen import er startet; dumpen er beholdt." >&2
    exit 1
fi
echo "Importmål: $local_database i $compose_project/$compose_database_service ($database_container, $docker_context)."

if [[ "$service_was_running" == true ]]; then
    echo "Stopper lokal $compose_application_service før import."
    if ! "${docker_command[@]}" stop "$service_container"; then
        echo "Kunne ikke stoppe $compose_application_service. Ingen import er startet; dumpen er beholdt." >&2
        exit 1
    fi
    local_service_stopped=true
fi
echo "Sletter og oppretter lokal $local_database før import."
local_import_started=true
if ! "${docker_command[@]}" exec "$database_container" sh -eu -c \
    'test "$MARIADB_DATABASE" = "$1"
     MYSQL_PWD="$MARIADB_ROOT_PASSWORD" exec mariadb --no-defaults -uroot -e "DROP DATABASE \`$1\`; CREATE DATABASE \`$1\`;"' \
    sh "$local_database"; then
    echo "Kunne ikke nullstille lokal $local_database. Databasen kan være delvis nullstilt; dumpen er beholdt." >&2
    exit 1
fi
if ! "${docker_command[@]}" exec -i "$database_container" sh -eu -c \
    'test "$MARIADB_DATABASE" = "$1"
     MYSQL_PWD="$MARIADB_ROOT_PASSWORD" exec mariadb --no-defaults -uroot "$1"' \
    sh "$local_database" <"$output_file"; then
    echo "Import til $local_database mislyktes og kan være delvis gjennomført. Dumpen er beholdt." >&2
    exit 1
fi
local_import_started=false
if [[ "$service_was_running" == true ]]; then
    if ! "${docker_command[@]}" start "$service_container"; then
        echo "Dumpen er importert, men $compose_application_service kunne ikke startes. Dumpen er beholdt." >&2
        exit 1
    fi
    local_service_stopped=false
    echo "Dump importert til lokal $local_database; $compose_application_service er startet."
else
    echo "Dump importert til lokal $local_database."
fi

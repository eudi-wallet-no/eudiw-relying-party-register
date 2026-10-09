#!/usr/bin/env bash
set +x
set -euo pipefail

if [[ $# -eq 1 && ( "$1" == "--help" || "$1" == "-h" ) ]]; then
    echo "Bruk: bash scripts/dump-mysql.sh"
    exit 0
fi

if [[ $# -ne 0 ]]; then
    echo "Bruk: bash scripts/dump-mysql.sh" >&2
    exit 1
fi

namespace="digdir-dl-digital-lommebok-systest"
expected_server="https://api.eid-systest.norwayeast.aroapp.io:6443"
port=33067

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

echo "Kontrollerer innlogging mot systest..."
if ! oc --context="$context" --request-timeout=10s whoami </dev/null >/dev/null 2>&1; then
    echo "Kunne ikke bekrefte innlogging mot systest. Innloggingen kan ha utløpt, eller API-et er utilgjengelig." >&2
    echo "Kontroller nettverk/VPN og logg inn manuelt med oc login --server=$expected_server med et nytt token." >&2
    exit 1
fi

applications="$(printf '%s\n' eudiw-rp-register-service eudiw-ca-service \
    eudiw-issuer-server eudiw-status-list)"
if ! application="$(printf '%s\n' "$applications" | FZF_DEFAULT_OPTS='' FZF_DEFAULT_OPTS_FILE='' \
    fzf --no-multi --prompt='Applikasjon> ' --header="Database-dump fra systest")"; then
    echo "Applikasjonsvalg avbrutt eller mislyktes." >&2
    exit 1
fi

database=""
database_key=""
env_file=""
env_variable=""
case "$application" in
    eudiw-rp-register-service)
        pod_prefix="digital-lommebok-rp-register-mariadb-"
        database="rp_register"
        ;;
    eudiw-ca-service)
        env_variable="EUDIW_CA_SERVICE_ENV"
        env_file="${EUDIW_CA_SERVICE_ENV:-}"
        pod_prefix="digital-lommebok-ca-mariadb-"
        database_key="MARIADB_DATABASE"
        ;;
    eudiw-issuer-server)
        env_variable="EUDIW_ISSUER_SERVER_ENV"
        env_file="${EUDIW_ISSUER_SERVER_ENV:-}"
        pod_prefix="digital-lommebok-issuer-mariadb-"
        database_key="MARIADB_URL"
        ;;
    eudiw-status-list)
        env_variable="EUDIW_STATUS_LIST_ENV"
        env_file="${EUDIW_STATUS_LIST_ENV:-}"
        pod_prefix="digital-lommebok-status-list-mariadb-"
        database_key="MARIADB_URL"
        ;;
    *)
        echo "Ugyldig applikasjonsvalg." >&2
        exit 1
        ;;
esac

if [[ -z "$database" && ( -z "$env_file" || ! -f "$env_file" || ! -r "$env_file" ) ]]; then
    echo "Mangler lesbar env-fil via $env_variable. Sett opp eudiw-developer-secrets og start terminalen på nytt." >&2
    exit 1
fi

read_env_value() {
    local key="$1" line value="" found=false
    local pattern="^[[:space:]]*(export[[:space:]]+)?${key}[[:space:]]*=(.*)$"
    while IFS= read -r line || [[ -n "$line" ]]; do
        line="${line%$'\r'}"
        if [[ "$line" =~ $pattern ]]; then
            if [[ "$found" == true ]]; then
                echo "Flere verdier for $key. Kontroller env-filen i developer-secrets." >&2
                return 1
            fi
            found=true
            value="${BASH_REMATCH[2]}"
            value="${value#"${value%%[![:space:]]*}"}"
            value="${value%"${value##*[![:space:]]}"}"
            if [[ ${#value} -ge 2 && ( "$value" == \"*\" || "$value" == \'*\' ) ]]; then
                value="${value:1:${#value}-2}"
            elif [[ "$value" == \"* || "$value" == \'* ]]; then
                echo "Uavsluttet sitat for $key i developer-secrets." >&2
                return 1
            fi
        fi
    done <"$env_file"
    if [[ "$found" != true || -z "$value" || "$value" == *'${'* ]]; then
        echo "Mangler konkret verdi for $key. Sett opp env-filen i eudiw-developer-secrets og start terminalen på nytt." >&2
        return 1
    fi
    printf '%s' "$value"
}

if [[ -z "$database" ]]; then
    database="$(read_env_value "$database_key")"
fi
if [[ "$database_key" == *_URL ]]; then
    url_pattern='^(jdbc:)?(mariadb|mysql)://[^/]+/([^/?#]+)([?#].*)?$'
    if [[ ! "$database" =~ $url_pattern ]]; then
        echo "Ugyldig database-URL i $database_key. Kontroller developer-secrets." >&2
        exit 1
    fi
    database="${BASH_REMATCH[3]}"
fi
if [[ "$database" == -* || "$database" == *$'\n'* || "$database" == *$'\r'* ]]; then
    echo "Ugyldige databaseverdier. Kontroller developer-secrets." >&2
    exit 1
fi

if ! pod_names="$(oc --context="$context" --namespace="$namespace" --request-timeout=10s get pods \
    --field-selector=status.phase=Running -o custom-columns=NAME:.metadata.name --no-headers </dev/null)"; then
    echo "Kunne ikke hente podene i $namespace. Kontroller innlogging og tilgang." >&2
    exit 1
fi
mariadb_pods="$(printf '%s\n' "$pod_names" | awk -v prefix="$pod_prefix" 'index($1, prefix) == 1 { print $1 }')"
if [[ -z "$mariadb_pods" ]]; then
    echo "Fant ingen kjørende MariaDB-poder for $application med prefiks $pod_prefix i $namespace." >&2
    exit 1
fi

if ! pod="$(printf '%s\n' "$mariadb_pods" | FZF_DEFAULT_OPTS='' FZF_DEFAULT_OPTS_FILE='' \
    fzf --no-multi --prompt='MariaDB-pod> ' --header="$namespace ($context)")"; then
    echo "Podvalg avbrutt eller mislyktes. Ingen dump er startet." >&2
    exit 1
fi
if [[ -z "$pod" ]] || ! printf '%s\n' "$mariadb_pods" | grep -Fxq -- "$pod"; then
    echo "Ugyldig podvalg. Ingen dump er startet." >&2
    exit 1
fi

umask 077
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
forward_pid=""
forward_log=""
dump_file=""
local_service_stopped=false

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
    if [[ "$local_service_stopped" == true && ( "$exit_status" -eq 130 || "$exit_status" -eq 143 ) ]]; then
        echo "Lokal import avbrutt og kan være delvis gjennomført. Tjenesten forblir stoppet; dumpen er beholdt." >&2
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
    echo "Port-forward kunne ikke startes innen 30 sekunder. Kontroller innlogging og port $port." >&2
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

if [[ "$application" != "eudiw-rp-register-service" ]]; then
    exit 0
fi

if ! import_choice="$(printf '%s\n' 'Importer til lokal Docker' 'Lagre dump' \
    | FZF_DEFAULT_OPTS='' FZF_DEFAULT_OPTS_FILE='' fzf --no-multi --no-sort \
        --prompt='Lokal import> ' --header='Import SLETTER alle data i lokale rp_register_db (digital-lommebok-rp). Stopp IntelliJ-appen først.')"; then
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
docker_context="$(docker context show)"
docker_endpoint="$(docker context inspect "$docker_context" --format '{{.Endpoints.docker.Host}}')"
if [[ "$docker_endpoint" != unix://* || -n "${DOCKER_HOST:-}" ]]; then
    echo "Import krever lokal Docker-kontekst uten DOCKER_HOST. Dumpen er beholdt." >&2
    exit 1
fi
docker_command=(docker --context="$docker_context")

database_container="$("${docker_command[@]}" ps --quiet \
    --filter label=com.docker.compose.project=digital-lommebok-rp \
    --filter label=com.docker.compose.service=rp-register-db)"
service_container="$("${docker_command[@]}" ps --all --quiet \
    --filter label=com.docker.compose.project=digital-lommebok-rp \
    --filter label=com.docker.compose.service=rp-register-service)"
if [[ -z "$database_container" || "$database_container" == *$'\n'* ]]; then
    echo "Import krever én kjørende rp-register-db i digital-lommebok-rp. Dumpen er beholdt." >&2
    exit 1
fi
if [[ "$service_container" == *$'\n'* ]]; then
    echo "Fant flere rp-register-service i digital-lommebok-rp. Dumpen er beholdt." >&2
    exit 1
fi

service_was_running=false
if [[ -n "$service_container" ]]; then
    if ! service_was_running="$("${docker_command[@]}" inspect --format '{{.State.Running}}' "$service_container")"; then
        echo "Kunne ikke kontrollere tjenestens kjørestatus. Ingen import er startet; dumpen er beholdt." >&2
        exit 1
    fi
    if [[ "$service_was_running" != true && "$service_was_running" != false ]]; then
        echo "Ukjent kjørestatus for tjenesten. Ingen import er startet; dumpen er beholdt." >&2
        exit 1
    fi
fi

if ! "${docker_command[@]}" exec "$database_container" sh -eu -c \
    'test "$MARIADB_DATABASE" = rp_register_db
     test -n "$MARIADB_ROOT_PASSWORD"
     MYSQL_PWD="$MARIADB_ROOT_PASSWORD" exec mariadb --no-defaults --connect-timeout=10 -uroot rp_register_db -e "SELECT 1"' >/dev/null; then
    echo "Kunne ikke bekrefte lokal databaseinnlogging. Kontroller rp_register_db og root-passordet. Ingen import er startet; dumpen er beholdt." >&2
    exit 1
fi

if [[ "$service_was_running" == true ]]; then
    echo "Stopper lokal rp-register-service før import."
    if ! "${docker_command[@]}" stop "$service_container"; then
        echo "Kunne ikke stoppe tjenesten. Ingen import er startet; dumpen er beholdt." >&2
        exit 1
    fi
    local_service_stopped=true
fi
echo "Sletter og oppretter lokal rp_register_db før import."
if ! "${docker_command[@]}" exec "$database_container" sh -eu -c \
    'MYSQL_PWD="$MARIADB_ROOT_PASSWORD" exec mariadb --no-defaults -uroot -e "DROP DATABASE \`rp_register_db\`; CREATE DATABASE \`rp_register_db\`;"'; then
    echo "Kunne ikke nullstille lokal database. Tjenesten forblir stoppet; dumpen er beholdt." >&2
    exit 1
fi
if ! "${docker_command[@]}" exec -i "$database_container" sh -eu -c \
    'MYSQL_PWD="$MARIADB_ROOT_PASSWORD" exec mariadb --no-defaults -uroot rp_register_db' <"$output_file"; then
    echo "Import mislyktes og kan være delvis gjennomført. Tjenesten forblir stoppet; dumpen er beholdt." >&2
    exit 1
fi
if [[ "$service_was_running" == true ]]; then
    if ! "${docker_command[@]}" start "$service_container"; then
        echo "Dumpen er importert, men tjenesten kunne ikke startes. Dumpen er beholdt." >&2
        exit 1
    fi
    local_service_stopped=false
    echo "Dump importert til lokal rp_register_db; rp-register-service er startet."
else
    echo "Dump importert til lokal rp_register_db."
fi

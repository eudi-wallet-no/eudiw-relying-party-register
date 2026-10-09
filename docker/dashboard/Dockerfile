FROM nginxinc/nginx-unprivileged:stable-alpine

USER root
RUN apk add --no-cache curl jq
USER 101

COPY static /usr/share/nginx/html
COPY default.conf /etc/nginx/conf.d/default.conf
COPY poll-status.sh /app/poll-status.sh
COPY --chmod=755 start-status.sh /docker-entrypoint.d/40-status.sh

HEALTHCHECK --interval=30s --timeout=5s --start-period=5s --retries=3 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/ || exit 1

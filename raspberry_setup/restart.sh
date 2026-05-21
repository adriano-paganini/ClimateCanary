#!/bin/bash
# Usage:
#   ./restart.sh            — fresh database on every restart (default)
#   ./restart.sh --keep-db  — database persists across restarts
set -e
cd "$(dirname "$0")"

KEEP_DB=false
if [ "$1" = "--keep-db" ]; then
    KEEP_DB=true
fi

docker build -t pi-gateway .
docker rm -f pi-gateway 2>/dev/null || true

if [ "$KEEP_DB" = true ]; then
    mkdir -p /home/pi/.pi-gateway
    touch /home/pi/.pi-gateway/sensor.db
    docker run --name pi-gateway --net=host --privileged \
        -v /run/dbus:/run/dbus:ro \
        -v /home/pi/.pi-gateway/sensor.db:/app/sensor.db \
        pi-gateway
else
    docker run --name pi-gateway --net=host --privileged \
        -v /run/dbus:/run/dbus:ro \
        pi-gateway
fi

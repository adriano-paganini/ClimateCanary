#!/bin/bash
# Usage:
#   ./restart.sh  ... database persists across restarts (default)
#   ./restart.sh --fresh-db ... discard the database on restart
set -e
cd "$(dirname "$0")"

FRESH_DB=false
if [ "$1" = "--fresh-db" ]; then
    FRESH_DB=true
fi

docker build -t pi-gateway .
docker rm -f pi-gateway 2>/dev/null || true

mkdir -p /home/pi/pi-gateway
touch /home/pi/pi-gateway/app.log

if [ "$FRESH_DB" = true ]; then
    rm -f /home/pi/pi-gateway/sensor.db
    docker run --name pi-gateway --net=host --privileged \
        -v /run/dbus:/run/dbus:ro \
        -v /home/pi/conf.yml:/app/conf.yml \
        -v /home/pi/pi-gateway/app.log:/app/app.log \
        pi-gateway
else
    touch /home/pi/pi-gateway/sensor.db
    docker run --name pi-gateway --net=host --privileged \
        -v /run/dbus:/run/dbus:ro \
        -v /home/pi/conf.yml:/app/conf.yml \
        -v /home/pi/pi-gateway/sensor.db:/app/sensor.db \
        -v /home/pi/pi-gateway/app.log:/app/app.log \
        pi-gateway
fi

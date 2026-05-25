#!/bin/bash
set -e

if [ ! -f /home/pi/conf.yml ]; then
    echo "Error: /home/pi/conf.yml not found. Copy it to the Pi before starting."
    exit 1
fi

mkdir -p /home/pi/pi-gateway
touch /home/pi/pi-gateway/app.log /home/pi/pi-gateway/sensor.db
docker rm -f pi-gateway 2>/dev/null || true
docker run --name pi-gateway --net=host --privileged \
    -v /run/dbus:/run/dbus:ro \
    -v /home/pi/conf.yml:/app/conf.yml \
    -v /home/pi/pi-gateway/sensor.db:/app/sensor.db \
    -v /home/pi/pi-gateway/app.log:/app/app.log \
    pi-gateway

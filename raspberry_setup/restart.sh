#!/bin/bash
set -e
cd "$(dirname "$0")"
docker build -t pi-gateway .
docker rm -f pi-gateway 2>/dev/null || true
docker run --name pi-gateway --net=host --privileged -v /run/dbus:/run/dbus:ro pi-gateway

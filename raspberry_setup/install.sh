#!/bin/bash

set -e
cd "$(dirname "$0")"

SERVICE_NAME="pi-gateway"

if [ "$EUID" -ne 0 ]; then
    echo "Please run as root: sudo $0"
    exit 1
fi

cp pi-gateway.service /etc/systemd/system/
systemctl daemon-reload
systemctl enable "$SERVICE_NAME"

echo "Service installed and enabled. Run restart.sh to build and start."

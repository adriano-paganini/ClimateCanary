package at.qe.skeleton.models;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Operational status of a device", enumAsRef = true)
public enum DeviceStatus {

    ONLINE,
    OFFLINE,
    MAINTENANCE,
    DEGRADED,
    DECOMMISSIONED,
    AVAILABLE,
    CONNECTED,
    CONNECTION_FAILED
}

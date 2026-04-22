package at.qe.skeleton.sensorstation.dto;

import at.qe.skeleton.raspberrypi.model.DeviceStatus;

public record SensorStationDTO (
        Long id,
        String name,
        DeviceStatus deviceStatus,
        Float measurementsPerSec,
        Long raspberryPiId,
        Long roomId
) {}

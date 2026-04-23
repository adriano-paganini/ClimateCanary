package at.qe.skeleton.dtos;

import at.qe.skeleton.models.DeviceStatus;

public record SensorStationDTO (
        Long id,
        String name,
        DeviceStatus deviceStatus,
        Float measurementsPerSec,
        Long raspberryPiId,
        Long roomId
) {}

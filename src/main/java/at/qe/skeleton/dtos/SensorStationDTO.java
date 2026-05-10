package at.qe.skeleton.dtos;

import at.qe.skeleton.models.DeviceStatus;

public record SensorStationDTO (
        Long id,
        String name,
        String bleMac,
        DeviceStatus deviceStatus,
        Integer measurementInterval,
        Long raspberryPiId,
        Long roomId
) {}

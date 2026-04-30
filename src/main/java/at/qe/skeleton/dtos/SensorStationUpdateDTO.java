package at.qe.skeleton.dtos;

import at.qe.skeleton.models.DeviceStatus;

public record SensorStationUpdateDTO(
        Long raspberryPiId,
        Long roomId,
        String name,
        DeviceStatus deviceStatus
) {}
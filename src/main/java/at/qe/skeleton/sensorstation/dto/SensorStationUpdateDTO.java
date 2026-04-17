package at.qe.skeleton.sensorstation.dto;

import at.qe.skeleton.raspberrypi.model.DeviceStatus;

public record SensorStationUpdateDTO(
        Long raspberryPiId,
        Long roomId,
        String name,
        DeviceStatus deviceStatus,
        Float measurementsPerSec
) {}
package at.qe.skeleton.dtos;

import at.qe.skeleton.models.DeviceStatus;

import java.util.List;

public record RaspberryPiUpdateDTO(
        String ipAddress,
        String hostName,
        DeviceStatus deviceStatus,
        Long roomId,
        List<Long> sensorStationIds
) {}

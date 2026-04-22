package at.qe.skeleton.dtos;

import at.qe.skeleton.models.DeviceStatus;

import java.util.List;

public record RaspberryPiDTO(
        Long id,
        String hostName,
        String ipAddress,
        DeviceStatus deviceStatus,
        List<Long> sensorStationIds
) {}

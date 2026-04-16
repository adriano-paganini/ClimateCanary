package at.qe.skeleton.raspberrypi.dto;

import at.qe.skeleton.raspberrypi.model.DeviceStatus;

import java.util.List;

public record RaspberryPiDTO(
        Long id,
        String hostName,
        String ipAddress,
        DeviceStatus deviceStatus,
        List<Long> sensorStationIds
) {}

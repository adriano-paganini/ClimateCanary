package at.qe.skeleton.raspberrypi.dto;

import at.qe.skeleton.raspberrypi.model.DeviceStatus;

import java.util.List;

public record RaspberryPiUpdateDTO(
        String ipAddress,
        String hostName,
        DeviceStatus deviceStatus,
        Long roomId,
        List<Long> sensorStationIds
) {}

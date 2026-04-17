package at.qe.skeleton.sensorstation.dto;

import at.qe.skeleton.raspberrypi.model.DeviceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SensorStationCreateDTO (

        @NotBlank
        String name,

        @NotBlank
        DeviceStatus deviceStatus,

        @NotNull
        Float measurementsPerSec,

        @NotNull
        Long raspberryPiId
) {}

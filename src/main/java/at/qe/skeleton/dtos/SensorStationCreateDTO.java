package at.qe.skeleton.dtos;

import at.qe.skeleton.models.DeviceStatus;
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
        Long raspberryPiId,

        @NotNull
        Long roomId
) {}

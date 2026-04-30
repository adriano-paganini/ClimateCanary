package at.qe.skeleton.dtos;

import at.qe.skeleton.models.DeviceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SensorStationCreateDTO (

        @NotBlank
        String name,

        @NotBlank
        DeviceStatus deviceStatus,

        @NotBlank
        String bleMac,

        @NotNull
        Integer measurementInterval,

        @NotNull
        Long raspberryPiId,

        @NotNull
        Long roomId
) {}

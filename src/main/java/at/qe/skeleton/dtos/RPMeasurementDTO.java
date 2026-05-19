package at.qe.skeleton.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;


public record RPMeasurementDTO(

    @NotNull(message = "Timestamp is required")
    @PositiveOrZero(message = "Timestamp must be a String in valid ISO 8601 timestamp format")
    String timestamp,

    @NotNull(message = "Temperature is required")
    Float temperature,

    @NotNull(message = "Humidity is required")
    Float humidity,

    @NotNull(message = "Pressure is required")
    Float pressure,

    @NotNull(message = "Air quality is required")
    Float airQuality,

    @NotNull(message = "Room id is required")
    Long roomId,

    @NotNull(message = "SensorStation id is required")
    Long sensorStationId
){}
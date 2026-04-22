package at.qe.skeleton.dtos;

import at.qe.skeleton.models.Metric;

import java.time.LocalDateTime;
import java.util.List;

public record MeasurementDTO(
        Long id,
        LocalDateTime timestamp,
        Long measurement,
        Metric metric,
        Long roomId,
        Long sensorStationId,
        List<Long> thresholdViolationIds
) {}

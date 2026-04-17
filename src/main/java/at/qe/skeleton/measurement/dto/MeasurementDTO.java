package at.qe.skeleton.measurement.dto;

import at.qe.skeleton.climatehint.model.Metric;

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

package at.qe.skeleton.violation.dto;


import at.qe.skeleton.climatehint.model.Metric;
import at.qe.skeleton.violation.model.ViolationStatus;

import java.time.LocalDateTime;
import java.util.List;

public record ThresholdViolationDTO(
        Long id,
        Metric metric,
        Long value,
        ViolationStatus violationStatus,
        LocalDateTime endTime,
        LocalDateTime startTime,
        Long thresholdId,
        Long roomId,
        List<Long> measurementIds
) {}

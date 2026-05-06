package at.qe.skeleton.dtos;

import at.qe.skeleton.models.Metric;
import at.qe.skeleton.models.ViolationStatus;

import java.time.LocalDateTime;
import java.util.List;

public record ThresholdViolationUpdateDTO(
        Metric metric,
        Float value,
        ViolationStatus violationStatus,
        LocalDateTime endTime,
        LocalDateTime startTime,
        Long thresholdId,
        Long roomId,
        List<Long> measurementIds
) {}

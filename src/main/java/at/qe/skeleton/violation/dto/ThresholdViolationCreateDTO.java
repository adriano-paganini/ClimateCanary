package at.qe.skeleton.violation.dto;

import at.qe.skeleton.climatehint.model.Metric;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record ThresholdViolationCreateDTO(

        @NotNull
        Metric metric,

        @NotNull
        Long value,

        @NotNull
        LocalDateTime startTime,

        @NotNull
        Long thresholdId,

        @NotNull
        Long roomId,

        @NotNull
        List<Long> measurementIds
) {}

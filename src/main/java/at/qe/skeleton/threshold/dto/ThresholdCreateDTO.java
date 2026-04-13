package at.qe.skeleton.threshold.dto;

import at.qe.skeleton.climatehint.model.Metric;
import at.qe.skeleton.threshold.model.ThresholdType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ThresholdCreateDTO(

        @NotNull
        Long roomId,

        @NotNull
        Metric metric,

        @NotNull
        Long boundValue,

        @NotNull
        ThresholdType thresholdType,

        @Size(min = 0)
        List<Long> climateHintIds
) {}

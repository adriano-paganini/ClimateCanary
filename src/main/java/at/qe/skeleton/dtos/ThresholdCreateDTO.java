package at.qe.skeleton.dtos;

import at.qe.skeleton.models.Metric;
import at.qe.skeleton.models.ThresholdType;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ThresholdCreateDTO(

        @NotNull
        Long roomId,

        @NotNull
        Metric metric,

        @NotNull
        Float boundValue,

        @NotNull
        ThresholdType thresholdType,

        List<Long> climateHintIds
) {}

package at.qe.skeleton.dtos;

import at.qe.skeleton.model.Metric;
import at.qe.skeleton.model.ThresholdType;
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

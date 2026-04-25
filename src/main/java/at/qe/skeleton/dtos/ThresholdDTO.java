package at.qe.skeleton.dtos;

import at.qe.skeleton.models.Metric;
import at.qe.skeleton.models.ThresholdType;

import java.util.List;

public record ThresholdDTO(

        Long id,
        Long roomId,
        Metric metric,
        Long boundValue,
        ThresholdType thresholdType,
        List<Long> climateHintIds,
        Boolean enabled
) {}

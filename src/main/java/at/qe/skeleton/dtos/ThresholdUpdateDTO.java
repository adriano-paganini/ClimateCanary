package at.qe.skeleton.dtos;

import at.qe.skeleton.models.Metric;
import at.qe.skeleton.models.ThresholdType;

import java.util.List;

public record ThresholdUpdateDTO(
        Long roomId,
        Metric metric,
        Float boundValue,
        ThresholdType thresholdType,
        List<Long> climateHintIds,
        Boolean enabled
) {}

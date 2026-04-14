package at.qe.skeleton.threshold.dto;

import at.qe.skeleton.climatehint.model.Metric;
import at.qe.skeleton.threshold.model.ThresholdType;

import java.util.List;

public record ThresholdUpdateDTO(
        Long roomId,
        Metric metric,
        Long boundValue,
        ThresholdType thresholdType,
        List<Long> climateHintIds,
        Boolean enabled
) {}

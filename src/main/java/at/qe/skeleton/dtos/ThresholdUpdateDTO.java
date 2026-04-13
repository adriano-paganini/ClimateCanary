package at.qe.skeleton.dtos;

import at.qe.skeleton.model.Metric;
import at.qe.skeleton.model.ThresholdType;

import java.util.List;

public record ThresholdUpdateDTO(
        Long roomId,
        Metric metric,
        Long boundValue,
        ThresholdType thresholdType,
        List<Long> climateHintIds,
        Boolean enabled
) {}

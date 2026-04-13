package at.qe.skeleton.dtos;

import at.qe.skeleton.model.Metric;

public record ClimateHintDTO(
        Long id,
        Metric metric,
        String hintText
) {}

package at.qe.skeleton.dtos;

import at.qe.skeleton.models.Metric;

public record ClimateHintDTO(
        Long id,
        Metric metric,
        String hintText
) {}

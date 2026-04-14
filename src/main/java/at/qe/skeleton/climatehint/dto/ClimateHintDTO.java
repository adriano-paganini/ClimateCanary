package at.qe.skeleton.climatehint.dto;

import at.qe.skeleton.climatehint.model.Metric;

public record ClimateHintDTO(
        Long id,
        Metric metric,
        String hintText
) {}

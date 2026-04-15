package at.qe.skeleton.climatehint.dto;

import at.qe.skeleton.climatehint.model.Metric;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ClimateHintCreateDTO(

        @NotNull
        Metric metric,

        @NotBlank
        String hintText
) {}

package at.qe.skeleton.dtos;

import at.qe.skeleton.models.Metric;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ClimateHintCreateDTO(

        @NotNull
        Metric metric,

        @NotBlank
        String hintText
) {}

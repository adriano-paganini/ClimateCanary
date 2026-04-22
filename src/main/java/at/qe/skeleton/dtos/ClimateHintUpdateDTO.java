package at.qe.skeleton.dtos;

import at.qe.skeleton.models.Metric;
import jakarta.validation.constraints.Size;

public record ClimateHintUpdateDTO(

        Metric metric,

        @Size(min = 1, message = "Hint Text must not be blank if provided")
        String hintText
) {
}

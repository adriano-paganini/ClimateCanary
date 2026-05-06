package at.qe.skeleton.dtos;

import at.qe.skeleton.models.Metric;
import at.qe.skeleton.models.ViolationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ViolationResolvedDTO(

        @NotNull(message = "Metric must not be null")
        Metric metric,

        @NotNull(message = "Room ID must not be null")
        @Positive(message = "Room ID must be positive")
        Long roomId,

        @NotBlank(message = "End time must not be blank")
        String endTime

) {
    public ViolationStatus status() {
        return ViolationStatus.RESOLVED;
    }
}
package at.qe.skeleton.dtos;

import at.qe.skeleton.models.Metric;
import at.qe.skeleton.models.ViolationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ViolationActiveDTO(
        @NotNull(message = "Metric is required")
        Metric metric,

        @NotNull(message = "Room id is required")
        @Positive(message = "Room id must be positive")
        Long roomId,

        @NotBlank(message = "Start time is required")
        String startTime,

        @NotNull(message = "Average value is required")
        Float avgValue
) {
    public ViolationStatus status() {
        return ViolationStatus.ACTIVE;
    }
}
package at.qe.skeleton.raspberrypi.dto;

import jakarta.validation.constraints.NotNull;

public record RaspberryPiCreateDTO(
        @NotNull
        Long roomId
) {
}

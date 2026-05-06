package at.qe.skeleton.dtos;

import jakarta.validation.constraints.NotNull;

public record RaspberryPiCreateDTO(
        @NotNull
        Long roomId,
        @NotNull
        String hostName
) {
}

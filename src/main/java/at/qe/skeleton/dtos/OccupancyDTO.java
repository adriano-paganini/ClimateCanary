package at.qe.skeleton.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OccupancyDTO(

        @NotBlank(message = "Room name must not be blank")
        String roomName,

        @NotNull(message = "Privacy mode must not be null")
        Boolean privacyMode

) {
}
package at.qe.skeleton.dtos;

import at.qe.skeleton.models.RoomType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RoomCreateDTO(

        @NotBlank
        String name,

        @NotNull
        RoomType roomType,

        @NotNull
        Boolean privacyMode,

        @NotNull
        Long departmentId,

        @NotNull
        Long buildingId
) {}

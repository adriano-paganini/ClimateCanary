package at.qe.skeleton.dtos;

import at.qe.skeleton.model.RoomType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RoomCreateDTO(

        @NotBlank
        String name,

        @NotNull
        RoomType roomType,

        @NotNull
        @Positive
        Integer minOccupancy,

        @NotNull
        Long departmentId,

        @NotNull
        Long buildingId
) {}

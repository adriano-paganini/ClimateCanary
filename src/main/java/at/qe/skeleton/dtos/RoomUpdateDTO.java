package at.qe.skeleton.dtos;

import at.qe.skeleton.model.RoomType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RoomUpdateDTO (
        @NotNull
        String name,

        RoomType roomType,

        @Positive
        Integer minOccupancy,

        @NotNull
        Long departmentId,

        @NotNull
        Long buildingId
){
}

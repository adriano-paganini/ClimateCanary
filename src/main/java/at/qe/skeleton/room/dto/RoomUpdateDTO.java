package at.qe.skeleton.room.dto;

import at.qe.skeleton.room.model.RoomType;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RoomUpdateDTO (

        @Size(min = 1, message = "Name must not be blank if provided")
        String name,

        RoomType roomType,

        @Positive
        Integer minOccupancy,

        Long departmentId,

        Long buildingId
){}

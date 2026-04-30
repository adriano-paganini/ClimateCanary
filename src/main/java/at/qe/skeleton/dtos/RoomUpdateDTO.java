package at.qe.skeleton.dtos;

import at.qe.skeleton.models.RoomType;
import jakarta.validation.constraints.Size;

public record RoomUpdateDTO (

        @Size(min = 1, message = "Name must not be blank if provided")
        String name,

        RoomType roomType,

        Boolean privacyMode,

        Long departmentId,

        Long buildingId
){}

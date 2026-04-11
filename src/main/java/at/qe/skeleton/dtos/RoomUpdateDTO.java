package at.qe.skeleton.dtos;

import at.qe.skeleton.model.RoomType;

public record RoomUpdateDTO (
        String name,
        RoomType roomType,
        Integer minOccupancy,
        Long departmentId
){
}

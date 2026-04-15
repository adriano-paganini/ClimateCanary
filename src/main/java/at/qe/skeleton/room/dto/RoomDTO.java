package at.qe.skeleton.room.dto;

import at.qe.skeleton.room.model.RoomType;

public record RoomDTO(

        Long id,
        String name,
        RoomType roomType,
        Integer minOccupancy,
        Long departmentId,
        Long buildingId,
        boolean active

) {}

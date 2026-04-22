package at.qe.skeleton.dtos;

import at.qe.skeleton.models.RoomType;

public record RoomDTO(

        Long id,
        String name,
        RoomType roomType,
        Integer minOccupancy,
        Long departmentId,
        Long buildingId,
        boolean active

) {}

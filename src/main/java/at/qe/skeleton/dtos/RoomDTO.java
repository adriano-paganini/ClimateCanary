package at.qe.skeleton.dtos;

import at.qe.skeleton.model.RoomType;

public record RoomDTO(
        Long id,
        String name,
        RoomType roomType,
        int minOccupancy,
        Long departmentId
) {
}

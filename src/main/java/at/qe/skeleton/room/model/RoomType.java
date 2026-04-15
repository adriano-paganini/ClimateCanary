package at.qe.skeleton.room.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Enumeration of available room types", enumAsRef = true)
public enum RoomType {

    OFFICE,
    COMMON_AREAS
}

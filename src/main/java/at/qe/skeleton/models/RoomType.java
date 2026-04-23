package at.qe.skeleton.models;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Enumeration of available room types", enumAsRef = true)
public enum RoomType {

    OFFICE,
    COMMON_AREAS
}

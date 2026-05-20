package at.qe.skeleton.models;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Type of employee absence", enumAsRef = true)
public enum AbsenceType {

    HOLIDAY,
    SICKNESS,
    PARENTAL_LEAVE,
    OTHER
}

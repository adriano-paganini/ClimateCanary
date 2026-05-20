package at.qe.skeleton.models;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Workflow status of an absence request", enumAsRef = true)
public enum AbsenceStatus {

    PLANNED,
    APPROVED,
    REJECTED,
    CANCELLED
}

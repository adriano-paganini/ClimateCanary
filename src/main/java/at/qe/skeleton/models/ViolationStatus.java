package at.qe.skeleton.models;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Lifecycle status of a threshold violation", enumAsRef = true)
public enum ViolationStatus {

    ACTIVE,
    RESOLVED,
    DISABLED
}

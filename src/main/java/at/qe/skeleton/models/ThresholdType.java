package at.qe.skeleton.models;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Direction of a threshold bound", enumAsRef = true)
public enum ThresholdType {

    LOWER,
    UPPER
}

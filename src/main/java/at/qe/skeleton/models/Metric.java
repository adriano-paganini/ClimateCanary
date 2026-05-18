package at.qe.skeleton.models;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Climate metric reported by a sensor station", enumAsRef = true)
public enum Metric {

    HUMIDITY,
    TEMPERATURE,
    PRESSURE,
    IAQ
}

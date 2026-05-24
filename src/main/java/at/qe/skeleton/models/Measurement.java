package at.qe.skeleton.models;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(indexes = {
        @Index(
                name = "idx_measurement_room_metric_timestamp_desc",
                columnList = "room_id, metric, timestamp DESC"
        ),
        @Index(
                name = "idx_measurement_room_metric_timestamp_asc",
                columnList = "room_id, metric, timestamp"
        )
})
public class Measurement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    private LocalDateTime timestamp;

    private Float measurement;

    @Enumerated(EnumType.STRING)
    private Metric metric;

    @ManyToOne
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne
    @JoinColumn(name = "sensorstation_id", nullable = false)
    private SensorStation sensorStation;

    @ManyToMany(mappedBy = "measurements")
    private List<ThresholdViolation> thresholdViolations = new ArrayList<>();

}

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

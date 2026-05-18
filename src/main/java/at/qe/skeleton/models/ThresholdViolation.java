package at.qe.skeleton.models;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@Table(name = "thresholdviolations")
@Entity
public class ThresholdViolation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Metric metric;

    @Column(name = "`value`")
    private Float value;

    @Enumerated(EnumType.STRING)
    private ViolationStatus violationStatus;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @ManyToOne(optional = false)
    @JoinColumn(name = "threshold_id")
    private Threshold threshold;

    @ManyToOne(optional = false)
    @JoinColumn(name = "room_id")
    private Room room;

    @ManyToMany
    @JoinTable(
            name = "threshold_violation_measurement",
            joinColumns = @JoinColumn(name = "threshold_violation_id"),
            inverseJoinColumns = @JoinColumn(name = "measurement_id")
    )
    private List<Measurement> measurements = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ThresholdViolation that = (ThresholdViolation) o;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }
}

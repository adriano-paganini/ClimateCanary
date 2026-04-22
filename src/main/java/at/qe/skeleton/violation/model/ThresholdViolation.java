package at.qe.skeleton.violation.model;

import at.qe.skeleton.climatehint.model.Metric;
import at.qe.skeleton.measurement.model.Measurement;
import at.qe.skeleton.room.model.Room;
import at.qe.skeleton.threshold.model.Threshold;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Table(name = "thresholdviolations")
@Entity
public class ThresholdViolation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Metric metric;

    @Column(name = "`value`")
    private Long value;

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

    public Long getId() {
        return id;
    }

    public Metric getMetric() {
        return metric;
    }

    public void setMetric(Metric metric) {
        this.metric = metric;
    }

    public Long getValue() {
        return value;
    }

    public void setValue(Long value) {
        this.value = value;
    }

    public ViolationStatus getViolationStatus() {
        return violationStatus;
    }

    public void setViolationStatus(ViolationStatus violationStatus) {
        this.violationStatus = violationStatus;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Threshold getThreshold() {
        return threshold;
    }

    public void setThreshold(Threshold threshold) {
        this.threshold = threshold;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public List<Measurement> getMeasurements() {
        return measurements;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ThresholdViolation that = (ThresholdViolation) o;
        return getId() == that.getId();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }
}

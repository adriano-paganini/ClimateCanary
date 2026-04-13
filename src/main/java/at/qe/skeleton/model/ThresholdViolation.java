package at.qe.skeleton.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Table(name = "thresholdviolations")
@Entity
public class ThresholdViolation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private Metric metric;
    private Long value;
    private ViolationStatus violationStatus;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public long getId() {
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

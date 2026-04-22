package at.qe.skeleton.threshold.model;

import at.qe.skeleton.climatehint.model.ClimateHint;
import at.qe.skeleton.climatehint.model.Metric;
import at.qe.skeleton.room.model.Room;
import at.qe.skeleton.violation.model.ThresholdViolation;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Table(name = "thresholds")
@Entity
public class Threshold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Metric metric;
    private boolean enabled;
    private Long boundValue;

    @Enumerated(EnumType.STRING)
    private ThresholdType thresholdType;

    @ManyToMany(mappedBy = "thresholds")
    private Set<ClimateHint> climateHints = new HashSet<>();

    @OneToMany(mappedBy = "threshold")
    private List<ThresholdViolation> violations = new ArrayList<>();

    @ManyToOne(optional = false)
    @JoinColumn(name = "room_id")
    private Room room;

    public Long getId() {
        return id;
    }

    public Metric getMetric() {
        return metric;
    }

    public void setMetric(Metric metric) {
        this.metric = metric;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Long getBoundValue() {
        return boundValue;
    }

    public void setBoundValue(Long boundValue) {
        this.boundValue = boundValue;
    }

    public ThresholdType getThresholdType() {
        return thresholdType;
    }

    public void setThresholdType(ThresholdType thresholdType) {
        this.thresholdType = thresholdType;
    }

    public Set<ClimateHint> getClimateHints() {
        return climateHints;
    }

    public void setClimateHints(Set<ClimateHint> climateHints) {
        this.climateHints = climateHints;
    }

    public List<ThresholdViolation> getViolations() {
        return violations;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Threshold)) return false;
        Threshold other = (Threshold) o;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

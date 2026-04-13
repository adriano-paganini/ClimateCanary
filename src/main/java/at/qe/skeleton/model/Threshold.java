package at.qe.skeleton.model;

import jakarta.persistence.*;

@Table(name = "thresholds")
@Entity
public class Threshold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Metric metric;
    private boolean enabled;
    private Long boundValue;
    private ThresholdType thresholdType;

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

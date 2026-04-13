package at.qe.skeleton.climatehint.model;

import at.qe.skeleton.threshold.model.Threshold;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Table(name = "climatehints")
@Entity
public class ClimateHint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Metric metric;
    private String hintText;

    @ManyToMany
    @JoinTable(
            name = "climatehint_threshold",
            joinColumns = @JoinColumn(name = "climatehint_id"),
            inverseJoinColumns = @JoinColumn(name = "threshold_id")
    )
    private Set<Threshold> thresholds = new HashSet<>();

    public Long getId() {
        return id;
    }

    public Metric getMetric() {
        return metric;
    }

    public void setMetric(Metric metric) {
        this.metric = metric;
    }

    public String getHintText() {
        return hintText;
    }

    public void setHintText(String hintText) {
        this.hintText = hintText;
    }

    public Set<Threshold> getThresholds() {
        return thresholds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClimateHint)) return false;
        ClimateHint other = (ClimateHint) o;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

package at.qe.skeleton.models;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Table(name = "climatehints")
@Entity
public class ClimateHint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClimateHint other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

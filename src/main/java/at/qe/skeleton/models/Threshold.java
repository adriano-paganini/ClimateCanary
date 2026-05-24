package at.qe.skeleton.models;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Table(name = "thresholds")
@Entity
public class Threshold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Metric metric;
    private boolean enabled;
    private Float boundValue;

    @Enumerated(EnumType.STRING)
    private ThresholdType thresholdType;

    @ManyToMany(mappedBy = "thresholds")
    private Set<ClimateHint> climateHints = new HashSet<>();

    @OneToMany(mappedBy = "threshold")
    private List<ThresholdViolation> violations = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id")
    private Room room;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Threshold other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

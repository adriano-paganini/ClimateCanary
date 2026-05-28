package at.qe.skeleton.models;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private RoomType roomType;

    private Boolean privacyMode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id")
    private Building building;

    private boolean active = true;

    @OneToMany(mappedBy = "room")
    private List<EmployeeProfile> employeeProfiles = new ArrayList<>();

    @OneToMany(mappedBy = "room")
    private List<Threshold> thresholds = new ArrayList<>();

    @OneToMany(mappedBy = "room")
    private List<ThresholdViolation> violations = new ArrayList<>();

    @OneToMany(mappedBy = "room")
    private List<SensorStation> sensorStations = new ArrayList<>();

    @OneToOne(mappedBy = "room", fetch = FetchType.LAZY)
    private RaspberryPi raspberryPi;

    @OneToMany(mappedBy = "room")
    private List<Measurement> measurements = new ArrayList<>();

    public Room() {}

    public Room(String name, RoomType roomType,  Boolean privacyMode) {
        this.name = name;
        this.roomType = roomType;
        this.privacyMode = privacyMode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Room other)) return false;

        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

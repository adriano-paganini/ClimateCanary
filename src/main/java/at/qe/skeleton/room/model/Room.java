package at.qe.skeleton.room.model;

import at.qe.skeleton.building.model.Building;
import at.qe.skeleton.department.model.Department;
import at.qe.skeleton.employeeprofile.model.EmployeeProfile;
import at.qe.skeleton.measurement.model.Measurement;
import at.qe.skeleton.raspberrypi.model.RaspberryPi;
import at.qe.skeleton.sensorstation.model.SensorStation;
import at.qe.skeleton.threshold.model.Threshold;
import at.qe.skeleton.violation.model.ThresholdViolation;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private RoomType roomType;

    private int minOccupancy;

    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne
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

    @OneToOne(mappedBy = "room")
    private RaspberryPi raspberryPi;

    @OneToMany(mappedBy = "room")
    private List<Measurement> measurements = new ArrayList<>();

    public Room() {}

    public Room(String name, RoomType roomType, int minOccupancy) {
        this.name = name;
        this.roomType = roomType;
        this.minOccupancy = minOccupancy;
    }

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public RoomType getRoomType() { return roomType; }
    public void setRoomType(RoomType roomType) { this.roomType = roomType; }

    public int getMinOccupancy() { return minOccupancy; }
    public void setMinOccupancy(int minOccupancy) { this.minOccupancy = minOccupancy; }

    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }

    public Building getBuilding() { return building; }
    public void setBuilding(Building building) { this.building = building; }

    public List<EmployeeProfile> getEmployeeProfiles() { return employeeProfiles; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public List<Threshold> getThresholds() {
        return thresholds;
    }

    public List<ThresholdViolation> getViolations() {
        return violations;
    }

    public List<SensorStation> getSensorStations() {
        return sensorStations;
    }

    public RaspberryPi getRaspberryPi() {
        return raspberryPi;
    }

    public void setRaspberryPi(RaspberryPi raspberryPi) {
        this.raspberryPi = raspberryPi;
    }

    public List<Measurement> getMeasurements() {
        return measurements;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Room)) return false;
        Room other = (Room) o;

        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

package at.qe.skeleton.model;

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

    @OneToMany(mappedBy = "room")
    private List<EmployeeProfile> employeeProfiles = new ArrayList<>();

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

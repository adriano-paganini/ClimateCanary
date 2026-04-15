package at.qe.skeleton.department.model;

import at.qe.skeleton.employeeprofile.model.EmployeeProfile;
import at.qe.skeleton.room.model.Room;
import at.qe.skeleton.userx.model.Userx;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "departments")
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToMany(mappedBy = "department")
    private List<Room> rooms = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "userx_id")
    private Userx departmentLeader;

    @OneToMany(mappedBy = "department")
    private List<EmployeeProfile> employeeProfiles = new ArrayList<>();

    public Department() {}

    public Department(String name) {
        this.name = name;
    }

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<Room> getRooms() { return rooms; }
    public void setRooms(List<Room> rooms) { this.rooms = rooms; }

    public Userx getDepartmentLeader() { return departmentLeader; }
    public void setDepartmentLeader(Userx departmentLeader) { this.departmentLeader = departmentLeader; }

    public List<EmployeeProfile> getEmployeeProfiles() { return employeeProfiles; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Department)) return false;
        Department other = (Department) o;

        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

package at.qe.skeleton.model;

import jakarta.persistence.*;

@Table(name = "employeeprofile")
@Entity
public class EmployeeProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "userx_id", nullable = false, unique = true)
    private Userx user;

    @ManyToOne
    @JoinColumn(name = "room_id")
    private Room room;

    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;

    public EmployeeProfile() {}

    public Long getId() { return id; }

    public Userx getUser() { return user; }
    public void setUser(Userx user) { this.user = user; }

    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }

    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmployeeProfile)) return false;
        EmployeeProfile other = (EmployeeProfile) o;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @PreRemove
    private void removeFromAssociations() {
        if (department != null) {
            department.getEmployeeProfiles().remove(this);
        }
        if (room != null) {
            room.getEmployeeProfiles().remove(this);
        }
    }
}


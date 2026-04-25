package at.qe.skeleton.models;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "absences")
public class Absence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    private AbsenceType absenceType;

    @Enumerated(EnumType.STRING)
    private AbsenceStatus absenceStatus;

    @ManyToOne
    @JoinColumn(name = "userx_id", nullable = false)
    private Userx user;

    public Absence() {}

    public Long getId() { return id; }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }

    public AbsenceType getAbsenceType() { return absenceType; }
    public void setAbsenceType(AbsenceType absenceType) { this.absenceType = absenceType; }

    public AbsenceStatus getAbsenceStatus() { return absenceStatus; }
    public void setAbsenceStatus(AbsenceStatus absenceStatus) { this.absenceStatus = absenceStatus; }

    public Userx getUser() { return user; }
    public void setUser(Userx user) { this.user = user; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Absence)) return false;
        Absence other = (Absence) o;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

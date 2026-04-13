package at.qe.skeleton.repositories;

import at.qe.skeleton.model.EmployeeProfile;
import at.qe.skeleton.model.Userx;

import java.util.Optional;

public interface EmployeeProfileRepository extends AbstractRepository<EmployeeProfile, Long> {
    void deleteById(Long id);

    Optional<EmployeeProfile> findByUser(Userx user);
}

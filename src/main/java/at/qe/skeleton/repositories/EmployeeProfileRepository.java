package at.qe.skeleton.repositories;

import at.qe.skeleton.common.AbstractRepository;
import at.qe.skeleton.models.EmployeeProfile;
import at.qe.skeleton.models.Userx;

import java.util.Optional;

public interface EmployeeProfileRepository extends AbstractRepository<EmployeeProfile, Long> {
    void deleteById(Long id);

    Optional<EmployeeProfile> findByUser(Userx user);
}

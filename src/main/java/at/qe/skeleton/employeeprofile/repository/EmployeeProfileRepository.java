package at.qe.skeleton.employeeprofile.repository;

import at.qe.skeleton.common.AbstractRepository;
import at.qe.skeleton.employeeprofile.model.EmployeeProfile;
import at.qe.skeleton.userx.model.Userx;

import java.util.Optional;

public interface EmployeeProfileRepository extends AbstractRepository<EmployeeProfile, Long> {
    void deleteById(Long id);

    Optional<EmployeeProfile> findByUser(Userx user);
}

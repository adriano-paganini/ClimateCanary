package at.qe.skeleton.repositories;

import at.qe.skeleton.common.AbstractRepository;
import at.qe.skeleton.models.EmployeeProfile;
import at.qe.skeleton.models.Userx;

import java.util.List;
import java.util.Optional;

public interface EmployeeProfileRepository extends AbstractRepository<EmployeeProfile, Long> {
    void deleteById(Long id);

    Optional<EmployeeProfile> findByUser(Userx user);

    List<EmployeeProfile> findByUser_Id(Long userId);

    List<EmployeeProfile> findByDepartment_Id(Long departmentId);

    List<EmployeeProfile> findByRoom_Id(Long roomId);

    long countByDepartment_Id(Long departmentId);

    long countByRoom_Id(Long roomId);

    List<EmployeeProfile> findByUser_IdAndDepartment_Id(Long userId, Long departmentId);

    Optional<EmployeeProfile> getByUser_Id(Long userId);
}

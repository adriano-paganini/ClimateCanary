package at.qe.skeleton.absence.repository;

import at.qe.skeleton.absence.model.Absence;
import at.qe.skeleton.common.AbstractRepository;
import at.qe.skeleton.userx.model.Userx;

import java.util.List;

public interface AbsenceRepository extends AbstractRepository<Absence, Long> {

    void deleteById(Long id);

    List<Absence> findByUser(Userx user);
}

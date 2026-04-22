package at.qe.skeleton.repositories;

import at.qe.skeleton.models.Absence;
import at.qe.skeleton.common.AbstractRepository;
import at.qe.skeleton.models.Userx;

import java.util.List;

public interface AbsenceRepository extends AbstractRepository<Absence, Long> {

    void deleteById(Long id);

    List<Absence> findByUser(Userx user);
}

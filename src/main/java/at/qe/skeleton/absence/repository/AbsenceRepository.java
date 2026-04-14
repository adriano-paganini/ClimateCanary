package at.qe.skeleton.absence.repository;

import at.qe.skeleton.absence.model.Absence;
import at.qe.skeleton.common.AbstractRepository;

public interface AbsenceRepository extends AbstractRepository<Absence, Long> {

    void deleteById(Long id);
}

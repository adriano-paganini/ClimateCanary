package at.qe.skeleton.repositories;

import at.qe.skeleton.model.Absence;

public interface AbsenceRepository extends AbstractRepository<Absence, Long> {

    void deleteById(Long id);
}

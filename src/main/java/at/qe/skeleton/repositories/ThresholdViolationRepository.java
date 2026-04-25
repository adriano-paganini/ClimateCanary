package at.qe.skeleton.repositories;

import at.qe.skeleton.common.AbstractRepository;
import at.qe.skeleton.models.ThresholdViolation;

public interface ThresholdViolationRepository extends AbstractRepository<ThresholdViolation, Long> {
    void deleteById(Long id);
}

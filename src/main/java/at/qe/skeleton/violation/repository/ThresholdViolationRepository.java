package at.qe.skeleton.violation.repository;

import at.qe.skeleton.common.AbstractRepository;
import at.qe.skeleton.violation.model.ThresholdViolation;

public interface ThresholdViolationRepository extends AbstractRepository<ThresholdViolation, Long> {
    void deleteById(Long id);
}

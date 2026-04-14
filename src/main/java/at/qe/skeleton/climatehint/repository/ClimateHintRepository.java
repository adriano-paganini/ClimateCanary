package at.qe.skeleton.climatehint.repository;

import at.qe.skeleton.common.AbstractRepository;
import at.qe.skeleton.climatehint.model.ClimateHint;

import java.util.List;

public interface ClimateHintRepository extends AbstractRepository<ClimateHint, Long> {
    List<ClimateHint> findAllById(Iterable<Long> ids);
}
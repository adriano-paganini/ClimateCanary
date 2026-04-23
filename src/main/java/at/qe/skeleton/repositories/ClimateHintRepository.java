package at.qe.skeleton.repositories;

import at.qe.skeleton.common.AbstractRepository;
import at.qe.skeleton.models.ClimateHint;

import java.util.List;

public interface ClimateHintRepository extends AbstractRepository<ClimateHint, Long> {
    List<ClimateHint> findAllById(Iterable<Long> ids);
}
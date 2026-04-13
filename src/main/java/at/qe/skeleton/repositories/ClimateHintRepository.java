package at.qe.skeleton.repositories;

import at.qe.skeleton.model.ClimateHint;
import jakarta.validation.constraints.Size;

import java.util.List;

public interface ClimateHintRepository extends AbstractRepository<ClimateHint, Long> {
    int findAllById(@Size(min = 0) List<Long> longs);
}
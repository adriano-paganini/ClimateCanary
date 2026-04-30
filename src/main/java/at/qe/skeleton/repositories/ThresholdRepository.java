package at.qe.skeleton.repositories;

import at.qe.skeleton.common.AbstractRepository;
import at.qe.skeleton.models.Metric;
import at.qe.skeleton.models.Threshold;

import java.util.List;

public interface ThresholdRepository extends AbstractRepository<Threshold, Long> {
    List<Threshold> findByRoom_IdAndMetric(Long roomId, Metric metric);

    List<Threshold> findByRoom_Id(Long roomId);

    List<Threshold> findByMetric(Metric metric);
}

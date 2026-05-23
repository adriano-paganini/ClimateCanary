package at.qe.skeleton.repositories;

import at.qe.skeleton.common.AbstractRepository;
import at.qe.skeleton.models.Metric;
import at.qe.skeleton.models.Threshold;
import at.qe.skeleton.models.ThresholdType;

import java.util.List;

public interface ThresholdRepository extends AbstractRepository<Threshold, Long> {
    List<Threshold> findByRoom_IdAndMetric(Long roomId, Metric metric);

    List<Threshold> findByRoom_Id(Long roomId);

    List<Threshold> findByMetric(Metric metric);

    boolean existsByRoomIdAndMetricAndThresholdType(Long roomId, Metric metric, ThresholdType thresholdType);

    boolean existsByRoomIdAndMetricAndThresholdTypeAndIdNot(Long roomId, Metric metric, ThresholdType thresholdType, Long id);
}

package at.qe.skeleton.sensorstation.repository;

import at.qe.skeleton.common.AbstractRepository;
import at.qe.skeleton.sensorstation.model.SensorStation;

import java.util.List;

public interface SensorStationRepository extends AbstractRepository<SensorStation, Long> {
    List<SensorStation> findAllById(Iterable<Long> longs);
}

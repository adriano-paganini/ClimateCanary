package at.qe.skeleton.sensorstation.repository;

import at.qe.skeleton.common.AbstractRepository;
import at.qe.skeleton.sensorstation.model.SensorStation;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SensorStationRepository extends AbstractRepository<SensorStation, Long> {
    List<SensorStation> findAllById(Iterable<Long> longs);

    void deleteById(Long id);

    @Query("SELECT r FROM SensorStation r WHERE r.room.active = true")
    List<SensorStation> findAllActive();
}

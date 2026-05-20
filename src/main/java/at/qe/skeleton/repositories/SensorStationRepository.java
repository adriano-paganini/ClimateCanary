package at.qe.skeleton.repositories;

import at.qe.skeleton.common.AbstractRepository;
import at.qe.skeleton.models.SensorStation;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SensorStationRepository extends AbstractRepository<SensorStation, Long> {
    void deleteById(Long id);

    @Query("SELECT r FROM SensorStation r WHERE r.room.active = true")
    List<SensorStation> findAllActive();

    Optional<SensorStation> findByBleMac(String bleMac);
}

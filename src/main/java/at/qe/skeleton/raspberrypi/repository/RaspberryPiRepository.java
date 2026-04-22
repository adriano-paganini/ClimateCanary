package at.qe.skeleton.raspberrypi.repository;

import at.qe.skeleton.common.AbstractRepository;
import at.qe.skeleton.raspberrypi.model.RaspberryPi;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RaspberryPiRepository extends AbstractRepository<RaspberryPi, Long> {
    void deleteById(Long id);

    @Query("SELECT r FROM RaspberryPi r WHERE r.room.active = true")
    List<RaspberryPi> findAllActive();
}
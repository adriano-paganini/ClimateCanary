package at.qe.skeleton.repositories;

import at.qe.skeleton.common.AbstractRepository;
import at.qe.skeleton.models.RaspberryPi;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RaspberryPiRepository extends AbstractRepository<RaspberryPi, Long> {
    void deleteById(Long id);
    Optional<RaspberryPi> findById(Long id);

    @Query("SELECT r FROM RaspberryPi r WHERE r.room.active = true")
    List<RaspberryPi> findAllActive();
}
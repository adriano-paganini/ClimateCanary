package at.qe.skeleton.room.repository;

import at.qe.skeleton.common.AbstractRepository;
import at.qe.skeleton.room.model.Room;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends AbstractRepository<Room, Long> {

    @Query("SELECT r FROM Room r WHERE r.id IN :ids AND r.active = true")
    List<Room> findAllByIdsAndActiveTrue(@Param("ids") List<Long> ids);

    List<Room> findAllByActiveTrue();

    Optional<Room> findByIdAndActiveTrue(Long id);

    void deleteById(Long id);
}

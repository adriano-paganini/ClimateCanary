package at.qe.skeleton.repositories;

import at.qe.skeleton.model.Room;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoomRepository extends AbstractRepository<Room, Long> {

    @Query("SELECT r FROM Room r WHERE r.id IN :ids AND r.active = true")
    List<Room> findAllByIdsAndActiveTrue(@Param("ids") List<Long> ids);

    List<Room> findAllByActiveTrue();

    void deleteById(Long id);
}

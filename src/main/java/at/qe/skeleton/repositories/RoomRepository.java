package at.qe.skeleton.repositories;

import at.qe.skeleton.model.Room;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Iterator;
import java.util.List;

public interface RoomRepository extends AbstractRepository<Room, Long> {

    @Query("SELECT r FROM Room r WHERE r.id IN :ids")
    List<Room> findAllByIds(@Param("ids") List<Long> ids);

    void deleteById(Long id);
}

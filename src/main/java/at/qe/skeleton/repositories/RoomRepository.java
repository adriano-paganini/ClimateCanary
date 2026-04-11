package at.qe.skeleton.repositories;

import at.qe.skeleton.model.Room;

import java.util.Iterator;
import java.util.List;

public interface RoomRepository extends AbstractRepository<Room, Long> {

    List<Room> findAllById(List<Long> ids);
}

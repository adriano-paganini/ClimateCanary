package at.qe.skeleton.services;

import at.qe.skeleton.exceptions.RoomNotFoundException;
import at.qe.skeleton.model.Room;
import at.qe.skeleton.repositories.RoomRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoomService {

    private final RoomRepository roomRepository;

    public RoomService(RoomRepository repo) {
        this.roomRepository = repo;
    }

    public List<Room> getAll() {
        return roomRepository.findAll();
    }

    public Room getById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new RoomNotFoundException("Room with id " + id + " not found"));
    }

    public Room create(Room room) {
        return roomRepository.save(room);
    }

    public Room update(Long id, Room updates) {

        Room existing = getById(id);

        if (updates.getName() != null) {
            existing.setName(updates.getName());
        }

        if (updates.getRoomType() != null) {
            existing.setRoomType(updates.getRoomType());
        }

        if (updates.getMinOccupancy() != 0) {
            existing.setMinOccupancy(updates.getMinOccupancy());
        }

        if (updates.getDepartment() != null) {
            existing.setDepartment(updates.getDepartment());
        }

        return roomRepository.save(existing);
    }

    public void delete(Long id) {
        roomRepository.deleteById(id);
    }
}

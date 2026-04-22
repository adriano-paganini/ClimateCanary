package at.qe.skeleton.services;

import at.qe.skeleton.common.exceptions.RaspberryPiNotFoundException;
import at.qe.skeleton.dtos.RaspberryPiUpdateDTO;
import at.qe.skeleton.models.RaspberryPi;
import at.qe.skeleton.repositories.RaspberryPiRepository;
import at.qe.skeleton.models.SensorStation;
import at.qe.skeleton.repositories.SensorStationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RaspberryPiService {

    private final RaspberryPiRepository repo;
    private final RoomService roomService;
    private final SensorStationRepository sensorStationRepository;

    public RaspberryPiService(RaspberryPiRepository repo, RoomService roomService, SensorStationRepository sensorStationRepository) {
        this.repo = repo;
        this.roomService = roomService;
        this.sensorStationRepository = sensorStationRepository;
    }

    public List<RaspberryPi> getAll() {
        return repo.findAllActive();
    }

    public RaspberryPi getById(Long id) {
        return repo.findById(id).orElseThrow(() -> new RaspberryPiNotFoundException("RaspberryPi with id " + id + " not found"));
    }

    public RaspberryPi create(RaspberryPi pi) {
        return repo.save(pi);
    }

    public RaspberryPi update(Long id, RaspberryPiUpdateDTO dto) {
        RaspberryPi existing = getById(id);

        if (dto.ipAddress() != null) {
            existing.setIpAddress(dto.ipAddress());
        }

        if (dto.deviceStatus() != null) {
            existing.setDeviceStatus(dto.deviceStatus());
        }

        if (dto.roomId() != null) {
            existing.setRoom(roomService.getById(dto.roomId()));
        }

        if (dto.sensorStationIds() != null) {
            List<SensorStation> stations = sensorStationRepository.findAllById(dto.sensorStationIds());

            existing.getSensorStations().forEach(s -> s.setRaspberryPi(null));
            existing.getSensorStations().clear();

            for (SensorStation s : stations) {
                s.setRaspberryPi(existing);
                existing.getSensorStations().add(s);
            }
        }

        return repo.save(existing);
    }

    public List<SensorStation> getSensorStations(Long raspberryPiId) {
        RaspberryPi pi = getById(raspberryPiId);
        return pi.getSensorStations();
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }
}

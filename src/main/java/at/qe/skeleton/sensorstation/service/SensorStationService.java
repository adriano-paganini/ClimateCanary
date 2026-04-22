package at.qe.skeleton.sensorstation.service;

import at.qe.skeleton.common.exceptions.SensorStationNotFoundException;
import at.qe.skeleton.raspberrypi.service.RaspberryPiService;
import at.qe.skeleton.room.service.RoomService;
import at.qe.skeleton.sensorstation.dto.SensorStationUpdateDTO;
import at.qe.skeleton.sensorstation.model.SensorStation;
import at.qe.skeleton.sensorstation.repository.SensorStationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SensorStationService {

    private final SensorStationRepository repo;
    private final RaspberryPiService raspberryPiService;
    private final RoomService roomService;

    public SensorStationService(SensorStationRepository repo,
                                RaspberryPiService raspberryPiService,
                                RoomService roomService) {
        this.repo = repo;
        this.raspberryPiService = raspberryPiService;
        this.roomService = roomService;
    }

    public List<SensorStation> getAll() {
        return repo.findAllActive();
    }

    public SensorStation getById(Long id) {
        return repo.findById(id).orElseThrow(() -> new SensorStationNotFoundException("SensorStation with id " + id + " not found"));
    }

    public SensorStation create(SensorStation s) {
        return repo.save(s);
    }

    public SensorStation update(Long id, SensorStationUpdateDTO dto) {

        SensorStation existing = getById(id);

        if (dto.name() != null) {
            existing.setName(dto.name());
        }

        if (dto.deviceStatus() != null) {
            existing.setDeviceStatus(dto.deviceStatus());
        }

        if (dto.measurementsPerSec() != null) {
            existing.setMeasurementsPerSec(dto.measurementsPerSec());
        }

        if (dto.raspberryPiId() != null) {
            existing.setRaspberryPi(raspberryPiService.getById(dto.raspberryPiId()));
        }

        if (dto.roomId() != null) {
            existing.setRoom(roomService.getById(dto.roomId()));
        }

        return repo.save(existing);
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }
}

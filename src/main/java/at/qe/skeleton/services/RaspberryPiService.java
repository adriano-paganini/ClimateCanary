package at.qe.skeleton.services;

import at.qe.skeleton.common.exceptions.NotFoundException;
import at.qe.skeleton.dtos.RaspberryPiUpdateDTO;
import at.qe.skeleton.models.RaspberryPi;
import at.qe.skeleton.repositories.RaspberryPiRepository;
import at.qe.skeleton.models.SensorStation;
import at.qe.skeleton.repositories.SensorStationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
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
        return repo.findById(id).orElseThrow(() -> new NotFoundException("RaspberryPi with id " + id + " not found"));
    }

    public RaspberryPi create(RaspberryPi pi) {
        RaspberryPi savedPi = repo.save(pi);

        log.info("Created raspberry pi with id={}", savedPi.getId());
        log.debug("Created raspberryPi details: id={}, ipAddress={}, deviceStatus={}, roomId={}",
                savedPi.getId(),
                savedPi.getIpAddress(),
                savedPi.getDeviceStatus(),
                savedPi.getRoom() != null ? savedPi.getRoom().getId() : null);

        return savedPi;
    }

    public RaspberryPi update(Long id, RaspberryPiUpdateDTO dto) {
        RaspberryPi existing = getById(id);

        StringBuilder debugInfo = new StringBuilder("Updated raspberry pi details:")
                .append(" id=").append(id);

        if (dto.ipAddress() != null) {
            existing.setIpAddress(dto.ipAddress());
            debugInfo.append(", ipAddress=").append(dto.ipAddress());
        }

        if (dto.deviceStatus() != null) {
            existing.setDeviceStatus(dto.deviceStatus());
            debugInfo.append(", deviceStatus=").append(dto.deviceStatus());
        }

        if (dto.roomId() != null) {
            existing.setRoom(roomService.getById(dto.roomId()));
            debugInfo.append(", roomId=").append(dto.roomId());
        }

        if (dto.sensorStationIds() != null) {
            List<SensorStation> stations = sensorStationRepository.findAllById(dto.sensorStationIds());

            existing.getSensorStations().forEach(s -> s.setRaspberryPi(null));
            existing.getSensorStations().clear();

            for (SensorStation s : stations) {
                s.setRaspberryPi(existing);
                existing.getSensorStations().add(s);
            }

            debugInfo.append(", sensorStationIds=").append(dto.sensorStationIds());
        }

        RaspberryPi updatedPi = repo.save(existing);

        log.info("Updated raspberry pi with id={}", id);
        log.debug(debugInfo.toString());

        return updatedPi;
    }

    public List<SensorStation> getSensorStations(Long raspberryPiId) {
        RaspberryPi pi = getById(raspberryPiId);
        return pi.getSensorStations();
    }

    public void delete(Long id) {
        repo.deleteById(id);
        log.info("Deleted raspberry pi with id={}", id);
    }
}
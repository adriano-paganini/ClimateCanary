package at.qe.skeleton.services;

import at.qe.skeleton.common.exceptions.SensorStationNotFoundException;
import at.qe.skeleton.dtos.SensorStationUpdateDTO;
import at.qe.skeleton.models.SensorStation;
import at.qe.skeleton.repositories.SensorStationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
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
        SensorStation savedStation = repo.save(s);

        log.info("Created sensor station with id={}", savedStation.getId());
        log.debug("SensorStation details: id={}, name={}, deviceStatus={}, measurementsPerSec={}, raspberryPiId={}, roomId={}",
                savedStation.getId(),
                savedStation.getName(),
                savedStation.getDeviceStatus(),
                savedStation.getMeasurementsPerSec(),
                savedStation.getRaspberryPi() != null ? savedStation.getRaspberryPi().getId() : null,
                savedStation.getRoom() != null ? savedStation.getRoom().getId() : null);

        return savedStation;
    }

    public SensorStation update(Long id, SensorStationUpdateDTO dto) {

        SensorStation existing = getById(id);

        StringBuilder debugInfo = new StringBuilder("Updated sensor station details:")
                .append(" id=").append(id);

        if (dto.name() != null) {
            existing.setName(dto.name());
            debugInfo.append(", name=").append(dto.name());
        }

        if (dto.deviceStatus() != null) {
            existing.setDeviceStatus(dto.deviceStatus());
            debugInfo.append(", deviceStatus=").append(dto.deviceStatus());
        }

        if (dto.measurementsPerSec() != null) {
            existing.setMeasurementsPerSec(dto.measurementsPerSec());
            debugInfo.append(", measurementsPerSec=").append(dto.measurementsPerSec());
        }

        if (dto.raspberryPiId() != null) {
            existing.setRaspberryPi(raspberryPiService.getById(dto.raspberryPiId()));
            debugInfo.append(", raspberryPiId=").append(dto.raspberryPiId());
        }

        if (dto.roomId() != null) {
            existing.setRoom(roomService.getById(dto.roomId()));
            debugInfo.append(", roomId=").append(dto.roomId());
        }

        SensorStation updatedStation = repo.save(existing);

        log.info("Updated sensor station with id={}", id);
        log.debug(debugInfo.toString());

        return updatedStation;
    }

    public void delete(Long id) {
        repo.deleteById(id);
        log.info("Deleted sensor station with id={}", id);
    }
}
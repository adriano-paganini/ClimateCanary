package at.qe.skeleton.services;

import at.qe.skeleton.common.exceptions.NotFoundException;
import at.qe.skeleton.dtos.RaspberryPiUpdateDTO;
import at.qe.skeleton.models.DeviceStatus;
import at.qe.skeleton.models.RaspberryPi;
import at.qe.skeleton.repositories.RaspberryPiRepository;
import at.qe.skeleton.models.SensorStation;
import at.qe.skeleton.repositories.SensorStationRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'BUILDING_ADMIN')")
    public List<RaspberryPi> getAll() {
        return repo.findAllActive();
    }

    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'BUILDING_ADMIN')")
    public RaspberryPi getById(Long id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("RaspberryPi with id " + id + " not found"));
    }

    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
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

    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'BUILDING_ADMIN')")
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

    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'BUILDING_ADMIN')")
    public List<SensorStation> getSensorStations(Long raspberryPiId) {
        RaspberryPi pi = getById(raspberryPiId);
        return pi.getSensorStations();
    }

    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public void delete(Long id) {
        repo.deleteById(id);
        log.info("Deleted raspberry pi with id={}", id);
    }

    @Transactional
    public void addAvailableSensorStations(Long id, List<String> stationBleMacs){
        //this is intended, as we still need to manage RPi authentication.
        RaspberryPi pi = getById(id);
        for (String mac : stationBleMacs) {
            SensorStation temp = sensorStationRepository.findByBleMac(mac).orElse(new SensorStation());
            temp.setBleMac(mac);
            temp.setRaspberryPi(pi);
            temp.setRoom(pi.getRoom());
            temp.setName(mac);
            temp.setDeviceStatus(DeviceStatus.AVAILABLE);
            sensorStationRepository.save(temp);
        }
    }
    @Transactional
    public void removeAvailableSensorStationAfterScanTimeOut(Long piId, Long stationId){
        RaspberryPi pi = getById(piId);
        SensorStation station = sensorStationRepository.findById(stationId).orElseThrow();
        pi.removeSensorStation(station);
        sensorStationRepository.delete(station);
    }
}
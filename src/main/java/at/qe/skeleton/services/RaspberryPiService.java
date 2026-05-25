package at.qe.skeleton.services;

import at.qe.skeleton.common.exceptions.NotFoundException;
import at.qe.skeleton.dtos.RaspberryPiUpdateDTO;
import at.qe.skeleton.models.DeviceStatus;
import at.qe.skeleton.models.RaspberryPi;
import at.qe.skeleton.models.Room;
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
    public List<RaspberryPi> getAllDecomissioned(){
        return repo.findAllByDecomissionedTrue();
    }

    public List<RaspberryPi> getAllInternal(){
        return repo.findAll();
    }

    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'BUILDING_ADMIN')")
    public RaspberryPi getById(Long id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("RaspberryPi with id " + id + " not found"));
    }

    public RaspberryPi getByIdInternal(Long id){
        return repo.findById(id).orElseThrow(() -> new NotFoundException("RaspberryPi with id " + id + " not found"));
    }

    @Transactional
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public RaspberryPi create(RaspberryPi pi) {
        if (pi.getRoom() == null || pi.getRoom().getId() == null) {
            throw new IllegalArgumentException("RaspberryPi must have a room id");
        }

        Room room = roomService.getById(pi.getRoom().getId());

        if (room.getRaspberryPi() != null) {
            throw new IllegalArgumentException("Room already has a RaspberryPi");
        }

        pi.setRoom(room);

        RaspberryPi savedPi = repo.save(pi);

        room.setRaspberryPi(savedPi);

        log.info("Created raspberry pi with id={}", savedPi.getId());
        log.debug("Created raspberryPi details: id={}, hostName={}, ipAddress={}, deviceStatus={}, roomId={}",
                savedPi.getId(),
                savedPi.getHostName(),
                savedPi.getIpAddress(),
                savedPi.getDeviceStatus(),
                savedPi.getRoom() != null ? savedPi.getRoom().getId() : null);

        return savedPi;
    }

    public void updateInternal(Long id, RaspberryPiUpdateDTO dto){
        sharedUpdate(id, dto);
    }

    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'BUILDING_ADMIN')")
    public RaspberryPi update(Long id, RaspberryPiUpdateDTO dto) {
        return sharedUpdate(id, dto);
    }

    private RaspberryPi sharedUpdate(Long id, RaspberryPiUpdateDTO dto){
        RaspberryPi existing = getByIdInternal(id);

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
        if (dto.hostName() != null) {
            existing.setHostName(dto.hostName());
            debugInfo.append(", hostName=").append(dto.hostName());
        }

        if (dto.roomId() != null) {
            var oldRoom = existing.getRoom();
            var newRoom = roomService.getById(dto.roomId());

            if (newRoom.getRaspberryPi() != null && !newRoom.getRaspberryPi().getId().equals(existing.getId())) {
                throw new IllegalArgumentException("Room already has a RaspberryPi");
            }

            if (oldRoom != null) {
                oldRoom.setRaspberryPi(null);
            }

            existing.setRoom(newRoom);
            newRoom.setRaspberryPi(existing);

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

    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'BUILDING_ADMIN')")
    public List<SensorStation> getAvailableSensorStations(Long raspberryPiId) {
        RaspberryPi pi = getById(raspberryPiId);
        return pi.getSensorStations()
                .stream()
                .filter(station -> station.getDeviceStatus() == DeviceStatus.AVAILABLE)
                .toList();
    }

    @Transactional
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public void delete(Long id) {
        RaspberryPi pi = getById(id);

        if (pi.getRoom() != null) {
            pi.getRoom().setRaspberryPi(null);
            pi.setRoom(null);
        }

        pi.getSensorStations().forEach(station -> station.setDeviceStatus(DeviceStatus.DECOMMISSIONED));
        pi.setDeviceStatus(DeviceStatus.DECOMMISSIONED);
        repo.save(pi);
        log.info("Deleted raspberry pi with id={}", id);
    }

    @Transactional
    public void addAvailableSensorStations(Long id, List<String> stationBleMacs) {
        RaspberryPi pi = getByIdInternal(id);

        log.info("Adding available sensor stations to raspberry pi with id={}", id);
        log.debug("Discovered sensor station BLE MACs for raspberry pi id={}: {}", id, stationBleMacs);

        for (String mac : stationBleMacs) {
            SensorStation temp = sensorStationRepository.findByBleMac(mac).orElse(new SensorStation());

            boolean existingStation = temp.getId() != null;

            temp.setBleMac(mac);
            temp.setRaspberryPi(pi);
            temp.setRoom(pi.getRoom());
            temp.setName(mac);
            temp.setDeviceStatus(DeviceStatus.AVAILABLE);
            if (temp.getMeasurementInterval() == null) {
                temp.setMeasurementInterval(60);
            }

            SensorStation savedStation = sensorStationRepository.save(temp);

            if (existingStation) {
                log.debug(
                        "Updated existing available sensor station: id={}, bleMac={}, raspberryPiId={}, roomId={}",
                        savedStation.getId(),
                        savedStation.getBleMac(),
                        id,
                        savedStation.getRoom() != null ? savedStation.getRoom().getId() : null
                );
            } else {
                log.debug(
                        "Created new available sensor station: id={}, bleMac={}, raspberryPiId={}, roomId={}",
                        savedStation.getId(),
                        savedStation.getBleMac(),
                        id,
                        savedStation.getRoom() != null ? savedStation.getRoom().getId() : null
                );
            }
        }

        log.info("Finished adding {} available sensor station(s) to raspberry pi with id={}", stationBleMacs.size(), id);
    }

    @Transactional
    public void removeAvailableSensorStationAfterScanTimeOut(Long piId, Long stationId) {
        log.info("Removing available sensor station after scan timeout: raspberryPiId={}, sensorStationId={}", piId, stationId);

        RaspberryPi pi = getByIdInternal(piId);
        SensorStation station = sensorStationRepository.findById(stationId).orElseThrow();

        log.debug(
                "Removing sensor station details before deletion: id={}, bleMac={}, name={}, deviceStatus={}, raspberryPiId={}, roomId={}",
                station.getId(),
                station.getBleMac(),
                station.getName(),
                station.getDeviceStatus(),
                station.getRaspberryPi() != null ? station.getRaspberryPi().getId() : null,
                station.getRoom() != null ? station.getRoom().getId() : null
        );

        pi.removeSensorStation(station);
        sensorStationRepository.delete(station);

        log.info("Deleted available sensor station with id={} after scan timeout for raspberry pi id={}", stationId, piId);
    }
}

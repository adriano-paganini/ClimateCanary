package at.qe.skeleton.services;

import at.qe.skeleton.common.exceptions.NotFoundException;
import at.qe.skeleton.dtos.SensorStationDTO;
import at.qe.skeleton.dtos.SensorStationUpdateDTO;
import at.qe.skeleton.mappers.SensorStationMapper;
import at.qe.skeleton.models.DeviceStatus;
import at.qe.skeleton.models.RaspberryPi;
import at.qe.skeleton.models.SensorStation;
import at.qe.skeleton.repositories.SensorStationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class SensorStationService {

    private final SensorStationRepository repo;
    private final RaspberryPiService raspberryPiService;
    private final RoomService roomService;
    private final SensorStationMapper sensorStationMapper;
    private final RaspberryPiServerService raspberryPiServerService;
    private static final String SENSOR_STATION_WITH_ID = "SensorStation with id: ";

    public SensorStationService(SensorStationRepository repo,
                                RaspberryPiService raspberryPiService,
                                RoomService roomService, SensorStationMapper sensorStationMapper, RaspberryPiServerService raspberryPiServerService) {
        this.repo = repo;
        this.raspberryPiService = raspberryPiService;
        this.roomService = roomService;
        this.sensorStationMapper = sensorStationMapper;
        this.raspberryPiServerService = raspberryPiServerService;
    }

    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'BUILDING_ADMIN')")
    public List<SensorStation> getAll() {
        return repo.findAllActive();
    }

    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'BUILDING_ADMIN')")
    public SensorStation getById(Long id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException(SENSOR_STATION_WITH_ID + id + " not found"));
    }


    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public SensorStation create(SensorStation s) {
        SensorStation savedStation = repo.save(s);

        log.info("Created sensor station with id={}", savedStation.getId());
        log.debug("Created sensorStation details: id={}, name={}, bleMac{}, deviceStatus={}, measurementInterval={}, raspberryPiId={}, roomId={}",
                savedStation.getId(),
                savedStation.getName(),
                savedStation.getBleMac(),
                savedStation.getDeviceStatus(),
                savedStation.getMeasurementInterval(),
                savedStation.getRaspberryPi() != null ? savedStation.getRaspberryPi().getId() : null,
                savedStation.getRoom() != null ? savedStation.getRoom().getId() : null);

        return savedStation;
    }

    public SensorStation update(Long piId,Long id, DeviceStatus status){
        SensorStation station = getByIdInternal(id);
        if (!Objects.equals(station.getRaspberryPi().getId(), piId)) throw new NotFoundException(SENSOR_STATION_WITH_ID + id + " not found");
        station.setDeviceStatus(status);
        return repo.save(station);
    }

    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'BUILDING_ADMIN')")
    public SensorStation update(Long id, SensorStationUpdateDTO dto) {
        SensorStation updated = internalUpdate(id, dto);
        SensorStation updatedStation = repo.save(updated);

        log.info("Updated sensor station with id={}", id);
        log.debug("Successfully saved the updated sensor station with id={}", updatedStation.getId());
        return updatedStation;
    }

    private SensorStation internalUpdate(Long id, SensorStationUpdateDTO dto) {
        SensorStation existing = getByIdInternal(id);

        StringBuilder debugInfo = new StringBuilder("Updating sensor station details: (Not yet saved)")
                .append(" id=").append(id);

        if (dto.name() != null) {
            existing.setName(dto.name());
            debugInfo.append(", name=").append(dto.name());
        }

        if (dto.deviceStatus() != null) {
            existing.setDeviceStatus(dto.deviceStatus());
            debugInfo.append(", deviceStatus=").append(dto.deviceStatus());
        }

        if (dto.raspberryPiId() != null) {
            existing.setRaspberryPi(raspberryPiService.getById(dto.raspberryPiId()));
            debugInfo.append(", raspberryPiId=").append(dto.raspberryPiId());
        }

        if (dto.roomId() != null) {
            existing.setRoom(roomService.getById(dto.roomId()));
            debugInfo.append(", roomId=").append(dto.roomId());
        }

        log.debug(debugInfo.toString());
        return existing;
    }

    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'BUILDING_ADMIN')")
    public SensorStation update(Long id, SensorStationUpdateDTO dto, Integer measurementInterval) {
        SensorStation existing = internalUpdate(id, dto);
        existing.setMeasurementInterval(measurementInterval);

        StringBuilder debugInfo = new StringBuilder("Updated sensor station details:")
                .append(" id=").append(id)
                .append(", measurementInterval=").append(measurementInterval);

        SensorStation updatedStation = repo.save(existing);

        if (updatedStation.getDeviceStatus() == DeviceStatus.AVAILABLE) {
            RaspberryPi pi = updatedStation.getRaspberryPi();

            if (pi == null) {
                throw new IllegalStateException("Cannot set up sensor station without assigned Raspberry Pi");
            }

            SensorStationDTO stationDTO = sensorStationMapper.mapTo(updatedStation);
            PiRequestResult result = raspberryPiServerService.setupStation(pi.getId(), stationDTO);
            debugInfo.append(", setupStationResult=").append(result);

            if (result == PiRequestResult.SUCCESS) {
                PiRequestResult connectResult = raspberryPiServerService.connectToStation(pi.getId(), stationDTO);
                debugInfo.append(", connectToStationResult=").append(connectResult);
            }
        }

        log.info("Updated sensor station with id={}", id);
        log.debug(debugInfo.toString());

        return updatedStation;
    }

    public SensorStation getByIdInternal(Long id) {
        return repo.findById(id).orElseThrow(()-> new NotFoundException(SENSOR_STATION_WITH_ID + id + "not found"));
    }
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public void delete(Long id) {
        repo.deleteById(id);
        log.info("Deleted sensor station with id={}", id);
    }
}

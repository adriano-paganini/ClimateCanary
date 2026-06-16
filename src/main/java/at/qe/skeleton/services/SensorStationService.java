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

/**
 * Service layer for managing {@link SensorStation} entities.
 *
 * <p>This service handles CRUD operations, state transitions, and synchronization
 * of sensor stations with associated Raspberry Pi devices.</p>
 *
 * <p>Responsibilities include:</p>
 * <ul>
 *     <li>Managing sensor station lifecycle (create, update, delete)</li>
 *     <li>Assigning stations to Raspberry Pi and rooms</li>
 *     <li>Synchronizing configuration with Raspberry Pi devices</li>
 *     <li>Handling device status transitions</li>
 * </ul>
 */
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

    /**
     * Updates the device status of a sensor station belonging to a specific Raspberry Pi.
     *
     * <p>This method ensures that the station is actually assigned to the given Pi.</p>
     *
     * @param piId Raspberry Pi ID
     * @param id sensor station ID
     * @param status new device status
     * @return updated sensor station
     * @throws NotFoundException if station does not belong to the given Pi
     */
    public SensorStation update(Long piId,Long id, DeviceStatus status){
        SensorStation station = getByIdInternal(id);
        if (!Objects.equals(station.getRaspberryPi().getId(), piId)) throw new NotFoundException(SENSOR_STATION_WITH_ID + id + " not found");
        station.setDeviceStatus(status);
        return repo.save(station);
    }

    /**
     * Updates sensor station properties using a DTO.
     *
     * @param id sensor station ID
     * @param dto update payload
     * @return updated sensor station
     */
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'BUILDING_ADMIN')")
    public SensorStation update(Long id, SensorStationUpdateDTO dto) {
        SensorStation updated = internalUpdate(id, dto);
        SensorStation updatedStation = repo.save(updated);

        log.info("Updated sensor station with id={}", id);
        log.debug("Successfully saved the updated sensor station with id={}", updatedStation.getId());
        return updatedStation;
    }

    /**
     * Applies updates from DTO to an existing sensor station without persisting changes.
     *
     * <p>This method only modifies the entity in memory. Persistence is handled by callers.</p>
     *
     * @param id sensor station ID
     * @param dto update data
     * @return modified sensor station entity (not yet saved)
     */
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

    /**
     * Updates a sensor station including measurement interval and triggers Raspberry Pi synchronization
     * if the station is in AVAILABLE state.
     *
     * <p>If the station is successfully set up on the Raspberry Pi, a follow-up connect request is sent.</p>
     *
     * @param id sensor station ID
     * @param dto update data
     * @param measurementInterval new measurement interval
     * @return updated sensor station
     */
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
            log.info("Sending setup request to Raspberry Pi {} for sensor station {} with bleMac={}, interval={}, roomId={}",
                    pi.getId(), stationDTO.id(), stationDTO.bleMac(), stationDTO.measurementInterval(), stationDTO.roomId());
            PiRequestResult result = raspberryPiServerService.setupStation(pi.getId(), stationDTO);
            debugInfo.append(", setupStationResult=").append(result);

            if (result == PiRequestResult.SUCCESS) {
                log.info("Setup request succeeded; sending follow-up connect request to Raspberry Pi {} for sensor station {}",
                        pi.getId(), stationDTO.id());
                PiRequestResult connectResult = raspberryPiServerService.connectToStation(pi.getId(), stationDTO);
                debugInfo.append(", connectToStationResult=").append(connectResult);
                log.info("Follow-up connect request result for sensor station {}: {}", stationDTO.id(), connectResult);
            }
        }

        log.info("Updated sensor station with id={}", id);
        log.debug(debugInfo.toString());

        return updatedStation;
    }

    /**
     * Internal lookup for a sensor station by ID without authorization checks.
     *
     * @param id sensor station ID
     * @return sensor station entity
     * @throws NotFoundException if not found
     */
    public SensorStation getByIdInternal(Long id) {
        return repo.findById(id).orElseThrow(()-> new NotFoundException(SENSOR_STATION_WITH_ID + id + "not found"));
    }

    /**
     * Soft-decommissions a sensor station by setting its status to {@link DeviceStatus#DECOMMISSIONED}.
     *
     * @param id sensor station ID
     */
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public void delete(Long id) {
        SensorStation station = getByIdInternal(id);
        station.setDeviceStatus(DeviceStatus.DECOMMISSIONED);
        repo.save(station);
        log.info("Deleted sensor station with id={}", id);
    }
}

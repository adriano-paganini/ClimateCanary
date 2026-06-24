package at.qe.skeleton.services;

import at.qe.skeleton.common.exceptions.NotFoundException;
import at.qe.skeleton.dtos.RPMeasurementDTO;
import at.qe.skeleton.mappers.RPMeasurementMapper;
import at.qe.skeleton.models.*;
import at.qe.skeleton.repositories.MeasurementRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service layer for managing measurements coming from sensor stations and Raspberry Pi devices.
 * <p>
 * Provides functionality for:
 * - Retrieving measurements (all or filtered by room, metric, and time range)
 * - Fetching latest measurements per metric for a room
 * - Persisting measurement data received from Raspberry Pi devices
 * <p>
 * Acts as the central business logic layer between controllers and the measurement repository.
 */
@Slf4j
@Service
public class MeasurementService {

    private final MeasurementRepository measurementRepository;
    private final SensorStationService sensorStationService;
    private final RaspberryPiService raspberryPiService;
    private final RoomService roomService;
    private final RPMeasurementMapper rPmeasurementMapper;

    public MeasurementService(MeasurementRepository measurementRepository, SensorStationService sensorStationService, RaspberryPiService raspberryPiService, RoomService roomService, RPMeasurementMapper rPmeasurementMapper) {
        this.measurementRepository = measurementRepository;
        this.sensorStationService = sensorStationService;
        this.raspberryPiService = raspberryPiService;
        this.roomService = roomService;
        this.rPmeasurementMapper = rPmeasurementMapper;
    }

    public List<Measurement> getAll() {
        return measurementRepository.findAll();
    }

    public Measurement getById(Long id) {
        return measurementRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Measurement with id " + id + " not found"));
    }

    public List<Measurement> getFiltered(Long roomId,
                                         Metric metric,
                                         LocalDateTime from,
                                         LocalDateTime to) {
        boolean hasRoom   = roomId != null;
        boolean hasMetric = metric != null;
        boolean hasRange  = from != null && to != null;

        log.debug("Filtering measurements with roomId={}, metric={}, from={}, to={}",
                roomId, metric, from, to);

        if (hasRoom && hasMetric && hasRange)
            return measurementRepository.findByRoomIdAndMetricAndTimestampBetween(roomId, metric, from, to);
        if (hasRoom && hasMetric)
            return measurementRepository.findByRoomIdAndMetric(roomId, metric);
        if (hasRoom && hasRange)
            return measurementRepository.findByRoomIdAndTimestampBetween(roomId, from, to);
        if (hasRoom)
            return measurementRepository.findByRoomId(roomId);
        if (hasRange)
            return measurementRepository.findByTimestampBetween(from, to);

        return measurementRepository.findAll();
    }

    /**
     * Retrieves the most recent measurement for each metric in a given room.
     *
     * @param roomId the room to query
     * @return map of metric -> latest measurement
     */
    public Map<Metric, Measurement> getLatestPerMetric(Long roomId) {
        Map<Metric, Measurement> latestMeasurements = Arrays.stream(Metric.values())
                .map(metric -> measurementRepository.findTopByRoomIdAndMetricOrderByTimestampDesc(roomId, metric))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(Measurement::getMetric, m -> m));

        log.debug("Retrieved latest measurements per metric for roomId={}, metricCount={}",
                roomId, latestMeasurements.size());

        return latestMeasurements;
    }

    /**
     * Stores measurements received from a Raspberry Pi device.
     * <p>
     * Validates that:
     * - The Raspberry Pi exists
     * - The sensor station belongs to the Raspberry Pi
     * - The referenced room exists
     * <p>
     * Then persists all measurements contained in the DTO.
     *
     * @param piId Raspberry Pi identifier
     * @param dto measurement payload from device
     * @throws NotFoundException if sensor station is not assigned to the given Raspberry Pi
     */
    @Transactional
    public void saveMeasurementsFromRaspberryPi(Long piId, RPMeasurementDTO dto){
         List<Measurement> measurements = rPmeasurementMapper.mapFrom(dto);

         RaspberryPi pi = raspberryPiService.getByIdInternal(piId);
         SensorStation station = sensorStationService.getByIdInternal(dto.sensorStationId());

        if (!(pi.getSensorStations().contains(station))){
            throw new NotFoundException("Sensor station not found for Raspberry Pi");
        }
        roomService.getById(dto.roomId());

        for (Measurement measurement : measurements) {
            measurementRepository.save(measurement);
        }
    }

}
package at.qe.skeleton.controllers;

import at.qe.skeleton.models.Metric;
import at.qe.skeleton.dtos.MeasurementDTO;
import at.qe.skeleton.mappers.MeasurementMapper;
import at.qe.skeleton.models.Measurement;
import at.qe.skeleton.services.MeasurementService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for managing measurements.
 * Provides endpoints for querying, filtering, and retrieving latest sensor data.
 */
@RestController
@RequestMapping("/api/measurement")
public class MeasurementController {

    private final MeasurementService measurementService;
    private final MeasurementMapper measurementMapper;

    public MeasurementController(MeasurementService measurementService,
                                 MeasurementMapper measurementMapper) {
        this.measurementService = measurementService;
        this.measurementMapper = measurementMapper;
    }

    /**
     * Retrieves measurements with optional filtering.
     * <p>
     * Supports filtering by:
     * - room ID
     * - metric type
     * - time range (from/to)
     * <p>
     * All filters are optional and can be combined.
     */
    @GetMapping
    public ResponseEntity<List<MeasurementDTO>> getAll(
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) Metric metric,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        List<MeasurementDTO> result = measurementService
                .getFiltered(roomId, metric, from, to)
                .stream()
                .map(measurementMapper::mapTo)
                .toList();

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MeasurementDTO> getById(@PathVariable Long id) {
        Measurement measurement = measurementService.getById(id);
        return ResponseEntity.ok(measurementMapper.mapTo(measurement));
    }

    /**
     * GET /api/measurement/room/{roomId}/latest
     * Returns the latest measurement for each metric in the room.
     */
    @GetMapping("/room/{roomId}/latest")
    public ResponseEntity<Map<String, MeasurementDTO>> getLatestPerMetric(@PathVariable Long roomId) {
        Map<String, MeasurementDTO> result = measurementService
                .getLatestPerMetric(roomId)
                .entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().name(),
                        e -> measurementMapper.mapTo(e.getValue())));
        return ResponseEntity.ok(result);
    }
}

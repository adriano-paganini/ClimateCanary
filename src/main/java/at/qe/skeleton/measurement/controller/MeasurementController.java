package at.qe.skeleton.measurement.controller;

import at.qe.skeleton.climatehint.model.Metric;
import at.qe.skeleton.measurement.dto.MeasurementDTO;
import at.qe.skeleton.measurement.mapper.MeasurementMapper;
import at.qe.skeleton.measurement.model.Measurement;
import at.qe.skeleton.measurement.service.MeasurementService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
     * GET /api/measurement
     * GET /api/measurement?roomId=1
     * GET /api/measurement?roomId=1&metric=TEMPERATURE
     * GET /api/measurement?from=2025-01-01T00:00:00&to=2025-12-31T23:59:59
     * GET /api/measurement?roomId=1&metric=TEMPERATURE&from=...&to=...
     * All parameters are optional and combinable.
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

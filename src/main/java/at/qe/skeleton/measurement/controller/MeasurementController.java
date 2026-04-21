package at.qe.skeleton.measurement.controller;

import at.qe.skeleton.measurement.dto.MeasurementDTO;
import at.qe.skeleton.measurement.mapper.MeasurementMapper;
import at.qe.skeleton.measurement.model.Measurement;
import at.qe.skeleton.measurement.service.MeasurementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @GetMapping
    public ResponseEntity<List<MeasurementDTO>> getAll() {

        List<MeasurementDTO> measurements = measurementService.getAll()
                .stream()
                .map(measurementMapper::mapTo)
                .toList();

        return ResponseEntity.ok(measurements);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MeasurementDTO> getById(@PathVariable Long id) {
        Measurement measurement = measurementService.getById(id);
        return ResponseEntity.ok(measurementMapper.mapTo(measurement));
    }
}

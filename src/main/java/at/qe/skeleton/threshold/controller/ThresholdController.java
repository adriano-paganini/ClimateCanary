package at.qe.skeleton.threshold.controller;

import at.qe.skeleton.threshold.dto.ThresholdCreateDTO;
import at.qe.skeleton.threshold.dto.ThresholdDTO;
import at.qe.skeleton.threshold.dto.ThresholdUpdateDTO;
import at.qe.skeleton.threshold.mapper.ThresholdMapper;
import at.qe.skeleton.threshold.model.Threshold;
import at.qe.skeleton.threshold.service.ThresholdService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/threshold")
public class ThresholdController {

    private final ThresholdService thresholdService;
    private final ThresholdMapper thresholdMapper;

    public ThresholdController(ThresholdService thresholdService,
                               ThresholdMapper thresholdMapper) {
        this.thresholdService = thresholdService;
        this.thresholdMapper = thresholdMapper;
    }

    @GetMapping
    public ResponseEntity<List<ThresholdDTO>> getAll() {
        List<ThresholdDTO> result = thresholdService.getAll()
                .stream()
                .map(thresholdMapper::mapTo)
                .toList();

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ThresholdDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(
                thresholdMapper.mapTo(thresholdService.getThresholdById(id))
        );
    }

    @PostMapping
    public ResponseEntity<ThresholdDTO> create(@Valid @RequestBody ThresholdCreateDTO dto) {
        Threshold created = thresholdService.create(dto);
        return ResponseEntity.ok(thresholdMapper.mapTo(created));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ThresholdDTO> update(@PathVariable Long id,
                                               @RequestBody ThresholdUpdateDTO dto) {
        return ResponseEntity.ok(
                thresholdMapper.mapTo(thresholdService.update(id, dto))
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        thresholdService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

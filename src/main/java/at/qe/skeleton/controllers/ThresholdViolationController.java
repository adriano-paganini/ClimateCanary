package at.qe.skeleton.controllers;

import at.qe.skeleton.dtos.ThresholdViolationCreateDTO;
import at.qe.skeleton.dtos.ThresholdViolationDTO;
import at.qe.skeleton.dtos.ThresholdViolationUpdateDTO;
import at.qe.skeleton.mappers.ThresholdViolationMapper;
import at.qe.skeleton.services.ThresholdViolationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/thresholdviolation ")
public class ThresholdViolationController {

    private final ThresholdViolationService thresholdViolationService;
    private final ThresholdViolationMapper thresholdViolationMapper;

    public ThresholdViolationController(ThresholdViolationService thresholdViolationService,
                                        ThresholdViolationMapper thresholdViolationMapper) {
        this.thresholdViolationService = thresholdViolationService;
        this.thresholdViolationMapper = thresholdViolationMapper;
    }

    @GetMapping
    public ResponseEntity<List<ThresholdViolationDTO>> getAll() {
        return ResponseEntity.ok(thresholdViolationService.findAll()
                .stream()
                .map(thresholdViolationMapper::mapTo)
                .toList()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ThresholdViolationDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(thresholdViolationMapper.mapTo(thresholdViolationService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<ThresholdViolationDTO> create(@Valid @RequestBody ThresholdViolationCreateDTO dto) {
        return ResponseEntity.ok(thresholdViolationMapper.mapTo(thresholdViolationService.create(dto)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ThresholdViolationDTO> update(@PathVariable Long id,
                                                        @Valid @RequestBody ThresholdViolationUpdateDTO dto) {
        return ResponseEntity.ok(thresholdViolationMapper.mapTo(thresholdViolationService.update(id, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        thresholdViolationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

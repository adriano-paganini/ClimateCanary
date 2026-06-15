package at.qe.skeleton.controllers;

import at.qe.skeleton.dtos.ThresholdViolationCreateDTO;
import at.qe.skeleton.dtos.ThresholdViolationDTO;
import at.qe.skeleton.dtos.ThresholdViolationUpdateDTO;
import at.qe.skeleton.mappers.ThresholdViolationMapper;
import at.qe.skeleton.models.ViolationStatus;
import at.qe.skeleton.services.ThresholdViolationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing threshold violations.
 * Supports retrieval with filtering and basic CRUD operations.
 */
@RestController
@RequestMapping("/api/thresholdviolation")
public class ThresholdViolationController {

    private final ThresholdViolationService thresholdViolationService;
    private final ThresholdViolationMapper thresholdViolationMapper;

    public ThresholdViolationController(ThresholdViolationService thresholdViolationService,
                                        ThresholdViolationMapper thresholdViolationMapper) {
        this.thresholdViolationService = thresholdViolationService;
        this.thresholdViolationMapper = thresholdViolationMapper;
    }

    /**
     * Returns threshold violations filtered by status, room, and department.
     * All parameters are optional and can be combined.
     */
    @GetMapping
    public ResponseEntity<List<ThresholdViolationDTO>> getAll(
            @RequestParam(required = false) ViolationStatus violationStatus,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) Long departmentId
    ) {
        return ResponseEntity.ok(thresholdViolationService.findAll(violationStatus, roomId, departmentId)
                .stream()
                .map(thresholdViolationMapper::mapToListItem)
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

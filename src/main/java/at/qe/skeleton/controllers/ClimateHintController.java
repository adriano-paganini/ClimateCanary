package at.qe.skeleton.controllers;

import at.qe.skeleton.dtos.ClimateHintCreateDTO;
import at.qe.skeleton.dtos.ClimateHintDTO;
import at.qe.skeleton.dtos.ClimateHintUpdateDTO;
import at.qe.skeleton.mappers.ClimateHintCreateMapper;
import at.qe.skeleton.mappers.ClimateHintMapper;
import at.qe.skeleton.models.ClimateHint;
import at.qe.skeleton.services.ClimateHintService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * REST controller for managing climate hints.
 * Provides CRUD operations for climate-related suggestions
 * used in the system.
 */
@RestController
@RequestMapping("/api/climatehint")
public class ClimateHintController {

    private final ClimateHintService climateHintService;
    private final ClimateHintMapper climateHintMapper;
    private final ClimateHintCreateMapper climateHintCreateMapper;

    public ClimateHintController(ClimateHintService climateHintService, ClimateHintMapper climateHintMapper, ClimateHintCreateMapper climateHintCreateMapper) {
        this.climateHintService = climateHintService;
        this.climateHintMapper = climateHintMapper;
        this.climateHintCreateMapper = climateHintCreateMapper;
    }

    @GetMapping
    public ResponseEntity<List<ClimateHintDTO>> getClimateHints() {
        List<ClimateHintDTO> climateHints = climateHintService.findAll()
                .stream()
                .map(climateHintMapper::mapTo)
                .toList();
        return ResponseEntity.ok(climateHints);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClimateHintDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(climateHintMapper.mapTo(climateHintService.getClimateHintById(id)));
    }

    @PostMapping
    public ResponseEntity<ClimateHintDTO> create(@Valid @RequestBody ClimateHintCreateDTO dto) {
        ClimateHint climateHint = climateHintService.create(climateHintCreateMapper.mapFrom(dto));

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(climateHint.getId())
                .toUri();

        return ResponseEntity.created(location).body(climateHintMapper.mapTo(climateHint));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ClimateHintDTO> update(@PathVariable Long id,
                                                 @Valid @RequestBody ClimateHintUpdateDTO dto) {
        ClimateHint updated = climateHintService.update(id, dto);
        return ResponseEntity.ok(climateHintMapper.mapTo(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        climateHintService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

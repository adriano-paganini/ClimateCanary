package at.qe.skeleton.controllers;

import at.qe.skeleton.dtos.SensorStationCreateDTO;
import at.qe.skeleton.dtos.SensorStationDTO;
import at.qe.skeleton.dtos.SensorStationUpdateDTO;
import at.qe.skeleton.mappers.SensorStationCreateMapper;
import at.qe.skeleton.mappers.SensorStationMapper;
import at.qe.skeleton.models.SensorStation;
import at.qe.skeleton.services.SensorStationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/sensorstation")
public class SensorStationController {

    private final SensorStationService sensorStationService;
    private final SensorStationMapper sensorStationMapper;
    private final SensorStationCreateMapper sensorStationCreateMapper;

    public SensorStationController(SensorStationService sensorStationService,
                                   SensorStationMapper sensorStationMapper,
                                   SensorStationCreateMapper sensorStationCreateMapper) {
        this.sensorStationService = sensorStationService;
        this.sensorStationMapper = sensorStationMapper;
        this.sensorStationCreateMapper = sensorStationCreateMapper;
    }

    @GetMapping
    public ResponseEntity<List<SensorStationDTO>> getAll() {
        List<SensorStationDTO> stations = sensorStationService.getAll()
                .stream()
                .map(sensorStationMapper::mapTo)
                .toList();
        return ResponseEntity.ok(stations);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SensorStationDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(sensorStationMapper.mapTo(sensorStationService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<SensorStationDTO> create(@RequestBody SensorStationCreateDTO dto) {

        SensorStation station = sensorStationService.create(sensorStationCreateMapper.mapFrom(dto));

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(station.getId())
                .toUri();

        return ResponseEntity.created(location).body(sensorStationMapper.mapTo(station));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<SensorStationDTO> update(@PathVariable Long id,
                                                   @Valid @RequestBody SensorStationUpdateDTO dto) {
        SensorStation updated = sensorStationService.update(id, dto);
        return ResponseEntity.ok(sensorStationMapper.mapTo(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sensorStationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

package at.qe.skeleton.controllers;

import at.qe.skeleton.dtos.SensorStationCreateDTO;
import at.qe.skeleton.dtos.SensorStationDTO;
import at.qe.skeleton.dtos.SensorStationUpdateDTO;
import at.qe.skeleton.mappers.SensorStationCreateMapper;
import at.qe.skeleton.mappers.SensorStationMapper;
import at.qe.skeleton.models.SensorStation;
import at.qe.skeleton.services.PiRequestResult;
import at.qe.skeleton.services.RaspberryPiServerService;
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
    private final RaspberryPiServerService raspberryPiServerService;

    public SensorStationController(SensorStationService sensorStationService,
                                   SensorStationMapper sensorStationMapper,
                                   SensorStationCreateMapper sensorStationCreateMapper, RaspberryPiServerService raspberryPiServerService) {
        this.sensorStationService = sensorStationService;
        this.sensorStationMapper = sensorStationMapper;
        this.sensorStationCreateMapper = sensorStationCreateMapper;
        this.raspberryPiServerService = raspberryPiServerService;
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
    @PatchMapping("/{id}/{measurementInterval}")
    public ResponseEntity<SensorStationDTO> initialUpdate(@PathVariable Long id,
                                                   @PathVariable Integer measurementInterval,
                                                   @Valid @RequestBody SensorStationUpdateDTO dto) {
        SensorStation updated = sensorStationService.update(id, dto, measurementInterval);
        return ResponseEntity.ok(sensorStationMapper.mapTo(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sensorStationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/find/{piId}")
    public ResponseEntity<Void> find(@PathVariable Long piId){
        if (PiRequestResult.SUCCESS== raspberryPiServerService.startScanForAvailableSensorStations(piId)){
            return ResponseEntity.accepted().build();
        }
        return ResponseEntity.badRequest().build();
    }
}

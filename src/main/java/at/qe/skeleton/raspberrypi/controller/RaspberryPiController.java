package at.qe.skeleton.raspberrypi.controller;

import at.qe.skeleton.raspberrypi.dto.RaspberryPiCreateDTO;
import at.qe.skeleton.raspberrypi.dto.RaspberryPiDTO;
import at.qe.skeleton.raspberrypi.dto.RaspberryPiUpdateDTO;
import at.qe.skeleton.raspberrypi.mapper.RaspberryPiCreateMapper;
import at.qe.skeleton.raspberrypi.mapper.RaspberryPiMapper;
import at.qe.skeleton.raspberrypi.model.RaspberryPi;
import at.qe.skeleton.raspberrypi.service.RaspberryPiService;
import at.qe.skeleton.sensorstation.dto.SensorStationDTO;
import at.qe.skeleton.sensorstation.mapper.SensorStationMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/bpi")
public class RaspberryPiController {

    private final RaspberryPiService raspberryPiService;
    private final RaspberryPiMapper raspberryPiMapper;
    private final RaspberryPiCreateMapper raspberryPiCreateMapper;
    private final SensorStationMapper sensorStationMapper;

    public RaspberryPiController(RaspberryPiService raspberryPiService,
                                 RaspberryPiMapper raspberryPiMapper,
                                 RaspberryPiCreateMapper raspberryPiCreateMapper,
                                 SensorStationMapper sensorStationMapper) {
        this.raspberryPiService = raspberryPiService;
        this.raspberryPiMapper = raspberryPiMapper;
        this.raspberryPiCreateMapper = raspberryPiCreateMapper;
        this.sensorStationMapper = sensorStationMapper;
    }

    @GetMapping
    public ResponseEntity<List<RaspberryPiDTO>> getAll() {
        List<RaspberryPiDTO> pis = raspberryPiService.getAll()
                .stream()
                .map(raspberryPiMapper::mapTo)
                .toList();
        return ResponseEntity.ok(pis);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RaspberryPiDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(raspberryPiMapper.mapTo(raspberryPiService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<RaspberryPiDTO> create(@Valid @RequestBody RaspberryPiCreateDTO dto) {

        RaspberryPi pi = raspberryPiService.create(raspberryPiCreateMapper.mapFrom(dto));

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(pi.getId())
                .toUri();

        return ResponseEntity.created(location).body(raspberryPiMapper.mapTo(pi));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<RaspberryPiDTO> update(@PathVariable Long id,
                                                 @Valid @RequestBody RaspberryPiUpdateDTO dto) {
        RaspberryPi updated = raspberryPiService.update(id, dto);
        return ResponseEntity.ok(raspberryPiMapper.mapTo(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        raspberryPiService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/sensorstations")
    public ResponseEntity<List<SensorStationDTO>> getSensorStations(@PathVariable Long id) {
        List<SensorStationDTO> stations = raspberryPiService.getSensorStations(id)
                .stream()
                .map(sensorStationMapper::mapTo)
                .toList();
        return ResponseEntity.ok(stations);
    }
}

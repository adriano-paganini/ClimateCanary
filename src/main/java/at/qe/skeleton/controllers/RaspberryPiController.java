package at.qe.skeleton.controllers;

import at.qe.skeleton.dtos.RaspberryPiCreateDTO;
import at.qe.skeleton.dtos.RaspberryPiDTO;
import at.qe.skeleton.dtos.RaspberryPiUpdateDTO;
import at.qe.skeleton.helper.PiConfigYamlBuilder;
import at.qe.skeleton.mappers.RaspberryPiCreateMapper;
import at.qe.skeleton.mappers.RaspberryPiMapper;
import at.qe.skeleton.models.RaspberryPi;
import at.qe.skeleton.services.RaspberryPiServerService;
import at.qe.skeleton.services.RaspberryPiService;
import at.qe.skeleton.dtos.SensorStationDTO;
import at.qe.skeleton.mappers.SensorStationMapper;
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
    private final RaspberryPiServerService raspberryPiServerService;
    private final PiConfigYamlBuilder piConfigYamlBuilder;

    public RaspberryPiController(RaspberryPiService raspberryPiService,
                                 RaspberryPiMapper raspberryPiMapper,
                                 RaspberryPiCreateMapper raspberryPiCreateMapper,
                                 SensorStationMapper sensorStationMapper, RaspberryPiServerService raspberryPiServerService, PiConfigYamlBuilder piConfigYamlBuilder) {
        this.raspberryPiService = raspberryPiService;
        this.raspberryPiMapper = raspberryPiMapper;
        this.raspberryPiCreateMapper = raspberryPiCreateMapper;
        this.sensorStationMapper = sensorStationMapper;
        this.raspberryPiServerService = raspberryPiServerService;
        this.piConfigYamlBuilder = piConfigYamlBuilder;
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
    public ResponseEntity<String> create(@Valid @RequestBody RaspberryPiCreateDTO dto) {
        RaspberryPi pi = raspberryPiService.create(raspberryPiCreateMapper.mapFrom(dto));

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(pi.getId())
                .toUri();

        return ResponseEntity.created(location).body(piConfigYamlBuilder.buildYaml(pi.getId()));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<RaspberryPiDTO> update(@PathVariable Long id,
                                                 @Valid @RequestBody RaspberryPiUpdateDTO dto) {
        RaspberryPi updated = raspberryPiService.update(id, dto);
        //send updated config to RPI
        raspberryPiServerService.sendConfig(id,piConfigYamlBuilder.buildYaml(id));
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

    @GetMapping("/{id}/availablesensorstations")
    public ResponseEntity<List<SensorStationDTO>> getAvailableSensorStations(@PathVariable Long id) {
        List<SensorStationDTO> stations = raspberryPiService.getAvailableSensorStations(id)
                .stream()
                .map(sensorStationMapper::mapTo)
                .toList();
        return ResponseEntity.ok(stations);
    }
}

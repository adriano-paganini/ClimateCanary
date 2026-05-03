package at.qe.skeleton.controllers;


import at.qe.skeleton.dtos.RPMeasurementDTO;
import at.qe.skeleton.dtos.RaspberryPiUpdateDTO;
import at.qe.skeleton.helper.PiConfigYamlBuilder;
import at.qe.skeleton.models.DeviceStatus;
import at.qe.skeleton.services.MeasurementService;
import at.qe.skeleton.services.RaspberryPiService;
import at.qe.skeleton.services.SensorStationService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/cpi")
public class RaspberryPiClientController {

    private final MeasurementService measurementService;
    private final RaspberryPiService raspberryPiService;
    private final SensorStationService sensorStationService;
    private final PiConfigYamlBuilder piConfigYamlBuilder;


    public RaspberryPiClientController(MeasurementService measurementService, RaspberryPiService raspberryPiService, SensorStationService sensorStationService, PiConfigYamlBuilder piConfigYamlBuilder) {
        this.measurementService = measurementService;
        this.raspberryPiService = raspberryPiService;
        this.sensorStationService = sensorStationService;
        this.piConfigYamlBuilder = piConfigYamlBuilder;
    }

    @PostMapping("/{piId}/measurements")
    public ResponseEntity<Void> receiveCurrentMeasurements(
            @PathVariable Long piId,
            @Valid @RequestBody RPMeasurementDTO dto
    ) {
        measurementService.saveMeasurementsFromRaspberryPi(piId, dto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{piId}/booted")
    public ResponseEntity<Void> piBooted(@PathVariable Long piId,
                                         @Valid @RequestBody RaspberryPiUpdateDTO dto) {
        raspberryPiService.update(piId,dto);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{piId}/config")
    public ResponseEntity<String> getConfigYaml(@PathVariable Long piId){
        String configYaml = piConfigYamlBuilder.buildYaml(piId);
        log.warn(configYaml);
        return ResponseEntity.ok(configYaml);
    }

    @PostMapping("/{piId}/discovered")
    public ResponseEntity<Void> receiveAvailableSensorStations(@PathVariable Long piId,
                                                               @RequestBody List<String> sensorStationBleMacs){
        raspberryPiService.addAvailableSensorStations(piId,sensorStationBleMacs);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{piId}/{sensorStationId}")
    public ResponseEntity<Void> updateSensorStationStatus(@PathVariable Long piId,
                                                          @PathVariable Long sensorStationId,
                                                          @RequestBody DeviceStatus status){
        sensorStationService.update(piId,sensorStationId,status);
        return ResponseEntity.ok().build();
    }
}

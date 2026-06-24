package at.qe.skeleton.controllers;


import at.qe.skeleton.dtos.RPMeasurementDTO;
import at.qe.skeleton.dtos.RaspberryPiUpdateDTO;
import at.qe.skeleton.dtos.ViolationActiveDTO;
import at.qe.skeleton.dtos.ViolationResolvedDTO;
import at.qe.skeleton.scheduled.AvailableSensorStationCleaner;
import at.qe.skeleton.helper.PiConfigYamlBuilder;
import at.qe.skeleton.models.DeviceStatus;
import at.qe.skeleton.services.MeasurementService;
import at.qe.skeleton.services.RaspberryPiService;
import at.qe.skeleton.services.SensorStationService;
import at.qe.skeleton.services.ThresholdViolationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * REST controller for Raspberry Pi client communication.
 * <p>
 * Handles:
 * - measurement ingestion from sensor stations
 * - device boot notifications
 * - configuration delivery
 * - sensor station discovery updates
 * - threshold violation reporting
 * <p>
 * This API is intended for Raspberry Pi devices and not for direct user access.
 */
@Slf4j
@RestController
@RequestMapping("/api/cpi")
public class RaspberryPiClientController {

    private final MeasurementService measurementService;
    private final RaspberryPiService raspberryPiService;
    private final SensorStationService sensorStationService;
    private final PiConfigYamlBuilder piConfigYamlBuilder;
    private final ThresholdViolationService thresholdViolationService;
    private final TaskScheduler taskScheduler;
    private final AvailableSensorStationCleaner availableSensorStationCleaner;

    public RaspberryPiClientController(MeasurementService measurementService, RaspberryPiService raspberryPiService, SensorStationService sensorStationService, PiConfigYamlBuilder piConfigYamlBuilder, ThresholdViolationService thresholdViolationService, TaskScheduler taskScheduler, AvailableSensorStationCleaner availableSensorStationCleaner) {
        this.measurementService = measurementService;
        this.raspberryPiService = raspberryPiService;
        this.sensorStationService = sensorStationService;
        this.piConfigYamlBuilder = piConfigYamlBuilder;
        this.thresholdViolationService = thresholdViolationService;
        this.taskScheduler = taskScheduler;
        this.availableSensorStationCleaner = availableSensorStationCleaner;
    }

    /**
     * Receives current measurements from a Raspberry Pi device.
     */
    @PostMapping("/{piId}/measurements")
    public ResponseEntity<Void> receiveCurrentMeasurements(
            @PathVariable Long piId,
            @Valid @RequestBody RPMeasurementDTO dto
    ) {
        measurementService.saveMeasurementsFromRaspberryPi(piId, dto);
        return ResponseEntity.ok().build();
    }

    /**
     * Called when a Raspberry Pi boots.
     * Updates internal device state and logs boot event.
     */
    @PostMapping("/{piId}/booted")
    public ResponseEntity<Void> piBooted(@PathVariable Long piId,
                                         @Valid @RequestBody RaspberryPiUpdateDTO dto,
                                         HttpServletRequest request) {
        log.info(
                "Received booted request: method={}, uri={}, remoteAddr={}, piId={}, body={}",
                request.getMethod(),
                request.getRequestURI(),
                request.getRemoteAddr(),
                piId,
                dto
        );
        raspberryPiService.updateInternal(piId,dto);
        return ResponseEntity.ok().build();
    }

    /**
     * Returns the YAML configuration for the Raspberry Pi device.
     */
    @GetMapping("/{piId}/config")
    public ResponseEntity<String> getConfigYaml(@PathVariable Long piId){
        String configYaml = piConfigYamlBuilder.buildYaml(piId);
        log.warn(configYaml);
        return ResponseEntity.ok(configYaml);
    }

    /**
     * Receives discovered BLE sensor stations from a Raspberry Pi.
     * Temporarily stores discovered devices and schedules cleanup after 5 minutes.
     */
    @PostMapping("/{piId}/discovered")
    public ResponseEntity<Void> receiveAvailableSensorStations(@PathVariable Long piId,
                                                               @RequestBody List<String> sensorStationBleMacs){
        log.info("Received available sensor stations: {}",sensorStationBleMacs);
        raspberryPiService.addAvailableSensorStations(piId,sensorStationBleMacs);

        taskScheduler.schedule(
                ()  -> availableSensorStationCleaner.cleanAvailableSensorStations(piId),
                Instant.now().plusSeconds(300)
        );

        return ResponseEntity.ok().build();
    }

    /**
     * Updates the status of a sensor station associated with a Raspberry Pi.
     */
    @PatchMapping("/{piId}/{sensorStationId}")
    public ResponseEntity<Void> updateSensorStationStatus(@PathVariable Long piId,
                                                          @PathVariable Long sensorStationId,
                                                          @RequestBody DeviceStatus status){
        sensorStationService.update(piId,sensorStationId,status);
        return ResponseEntity.ok().build();
    }

    /**
     * Reports an active threshold violation from a Raspberry Pi.
     */
    @PostMapping("/{piId}/violation")
    public ResponseEntity<Void> receiveActiveViolation(@PathVariable Long piId,
                                                       @Valid @RequestBody ViolationActiveDTO dto){
        thresholdViolationService.create(piId,dto);
        return ResponseEntity.ok().build();
    }

    /**
     * Marks a previously reported threshold violation as resolved.
     */
    @PatchMapping("/{piId}/violation/resolve")
    public ResponseEntity<Void> deactivateActiveViolation(@PathVariable Long piId,
                                                          @Valid @RequestBody ViolationResolvedDTO dto){
        thresholdViolationService.update(piId,dto);
        return ResponseEntity.ok().build();
    }
}

package at.qe.skeleton.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import at.qe.skeleton.services.SensorService;
import at.qe.skeleton.dtos.SensorDTO;

/*
Endpoint for Systemdurchstich, this is where the raspberry sends its 
sensor data. Here, raspberry is client, backend is server. 
*/

@RestController
@RequestMapping("/api/sensor")
public class SensorController {

    private final SensorService sensorService;

    public SensorController(SensorService sensorService) {
        this.sensorService = sensorService;
    }

    @PostMapping
    public ResponseEntity<Void> receiveSensorData(@RequestBody SensorDTO dto) {

        sensorService.processSensorData(dto);

        return ResponseEntity.ok().build();
    }
}
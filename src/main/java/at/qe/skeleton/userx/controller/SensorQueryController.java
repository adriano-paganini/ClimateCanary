package at.qe.skeleton.controllers;

import org.springframework.web.bind.annotation.*;
import at.qe.skeleton.services.SensorService;
import at.qe.skeleton.dtos.SensorDTO;

/* 
Endpoint for Systemdurchstich, to display the data sent by the raspberry
in the browser. 
 */

@RestController
@RequestMapping("/api/sensor/query")
public class SensorQueryController {

    private final SensorService sensorService;

    public SensorQueryController(SensorService sensorService) {
        this.sensorService = sensorService;
    }

    @GetMapping("/latest")
    public SensorDTO getLatestSensorData() {
        SensorDTO data = sensorService.getLatestSensorData();
        if (data == null) {
            return new SensorDTO(); 
        }
        return data;
    }
}
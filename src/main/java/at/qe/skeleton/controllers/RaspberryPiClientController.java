package at.qe.skeleton.controllers;


import at.qe.skeleton.dtos.RPMeasurementDTO;
import at.qe.skeleton.dtos.RaspberryPiUpdateDTO;
import at.qe.skeleton.services.MeasurementService;
import at.qe.skeleton.services.RaspberryPiService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/cpi")
public class RaspberryPiClientController {

    private final MeasurementService measurementService;
    private final RaspberryPiService raspberryPiService;

    public RaspberryPiClientController(MeasurementService measurementService, RaspberryPiService raspberryPiService) {
        this.measurementService = measurementService;
        this.raspberryPiService = raspberryPiService;
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
}

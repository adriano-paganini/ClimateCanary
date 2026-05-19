package at.qe.skeleton.scheduled;

import at.qe.skeleton.models.DeviceStatus;
import at.qe.skeleton.models.RaspberryPi;
import at.qe.skeleton.models.SensorStation;
import at.qe.skeleton.services.RaspberryPiService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AvailableSensorStationCleaner {

    private final RaspberryPiService raspberryPiService;

    public AvailableSensorStationCleaner(RaspberryPiService raspberryPiService) {
        this.raspberryPiService = raspberryPiService;
    }

    @Transactional
    public void cleanAvailableSensorStations(Long piId){
        RaspberryPi raspberryPi = raspberryPiService.getByIdInternal(piId);
        List<SensorStation> sensorStations = List.copyOf(raspberryPi.getSensorStations());
        for (SensorStation station : sensorStations){
            if (station.getDeviceStatus() == DeviceStatus.AVAILABLE){
                raspberryPiService.removeAvailableSensorStationAfterScanTimeOut(piId,station.getId());
            }
        }
    }
}

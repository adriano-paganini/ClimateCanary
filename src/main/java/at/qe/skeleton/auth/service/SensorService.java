package at.qe.skeleton.services;
import org.springframework.stereotype.Service;
import at.qe.skeleton.dtos.SensorDTO;

@Service
public class SensorService {

    private volatile SensorDTO latestSensorData;

    public void processSensorData(SensorDTO dto) {

        System.out.println("Received Sensor Data:");
        System.out.println("Timestamp: " + dto.getTimestamp());
        System.out.println("Temp: " + dto.getTemperature());
        System.out.println("Humidity: " + dto.getHumidity());
        System.out.println("Pressure: " + dto.getPressure());
        System.out.println("Gas: " + dto.getGasResistance());

        latestSensorData = dto;

    }

    public SensorDTO getLatestSensorData() {
        return latestSensorData;
    }
}
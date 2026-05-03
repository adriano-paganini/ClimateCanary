package at.qe.skeleton.mappers;

import at.qe.skeleton.common.DTOMapper;
import at.qe.skeleton.dtos.RPMeasurementDTO;
import at.qe.skeleton.models.Measurement;
import at.qe.skeleton.models.Metric;
import at.qe.skeleton.models.Room;
import at.qe.skeleton.models.SensorStation;
import at.qe.skeleton.services.RoomService;
import at.qe.skeleton.services.SensorStationService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
public class RPMeasurementMapper implements DTOMapper<List<Measurement>, RPMeasurementDTO> {

    private final RoomService roomService;
    private final SensorStationService sensorStationService;

    public RPMeasurementMapper(RoomService roomService1, SensorStationService sensorStationService) {
        this.roomService = roomService1;
        this.sensorStationService = sensorStationService;
    }

    @Override
    public RPMeasurementDTO mapTo(List<Measurement> entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Measurement> mapFrom(RPMeasurementDTO dto) {

        Room room = roomService.getById(dto.roomId());
        SensorStation sensorStation = sensorStationService.getById(dto.sensorStationId());
        //TODO: UPDATE TIMESTAMP LOGIC ACCORDING TO RPI
        LocalDateTime timestamp = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(dto.timestamp()),
                ZoneOffset.UTC
        );
        List<Measurement> measurementList = new ArrayList<>();
        Measurement tempMeasurement = new Measurement();
        tempMeasurement.setRoom(room);
        tempMeasurement.setMetric(Metric.TEMPERATURE);
        tempMeasurement.setMeasurement(dto.temperature());
        tempMeasurement.setTimestamp(timestamp);
        tempMeasurement.setThresholdViolations(new ArrayList<>());
        tempMeasurement.setSensorStation(sensorStation);
        measurementList.add(tempMeasurement);

        Measurement humidityMeasurement = new Measurement();
        humidityMeasurement.setRoom(room);
        humidityMeasurement.setMetric(Metric.HUMIDITY);
        humidityMeasurement.setMeasurement(dto.humidity());
        humidityMeasurement.setTimestamp(timestamp);
        humidityMeasurement.setThresholdViolations(new ArrayList<>());
        humidityMeasurement.setSensorStation(sensorStation);
        measurementList.add(humidityMeasurement);

        Measurement pressureMeasurement = new Measurement();
        pressureMeasurement.setRoom(room);
        pressureMeasurement.setMetric(Metric.PRESSURE);
        pressureMeasurement.setMeasurement(dto.pressure());
        pressureMeasurement.setTimestamp(timestamp);
        pressureMeasurement.setThresholdViolations(new ArrayList<>());
        pressureMeasurement.setSensorStation(sensorStation);
        measurementList.add(pressureMeasurement);

        Measurement iaqMeasurement = new Measurement();
        iaqMeasurement.setRoom(room);
        iaqMeasurement.setMetric(Metric.IAQ);
        iaqMeasurement.setMeasurement(dto.airQuality());
        iaqMeasurement.setTimestamp(timestamp);
        iaqMeasurement.setThresholdViolations(new ArrayList<>());
        iaqMeasurement.setSensorStation(sensorStation);
        measurementList.add(iaqMeasurement);

        return measurementList;
    }
}

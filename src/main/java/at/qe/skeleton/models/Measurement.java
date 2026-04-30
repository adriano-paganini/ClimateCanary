package at.qe.skeleton.models;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Measurement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime timestamp;

    private Float measurement;

    @Enumerated(EnumType.STRING)
    private Metric metric;

    @ManyToOne
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne
    @JoinColumn(name = "sensorstation_id", nullable = false)
    private SensorStation sensorStation;

    @ManyToMany(mappedBy = "measurements")
    private List<ThresholdViolation> thresholdViolations = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Float getMeasurement() {
        return measurement;
    }

    public void setMeasurement(Float measurement) {
        this.measurement = measurement;
    }

    public Metric getMetric() {
        return metric;
    }

    public void setMetric(Metric metric) {
        this.metric = metric;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public SensorStation getSensorStation() {
        return sensorStation;
    }

    public void setSensorStation(SensorStation sensorStation) {
        this.sensorStation = sensorStation;
    }

    public List<ThresholdViolation> getThresholdViolations() {
        return thresholdViolations;
    }

    public void setThresholdViolations(List<ThresholdViolation> thresholdViolations) {
        this.thresholdViolations = thresholdViolations;
    }
}

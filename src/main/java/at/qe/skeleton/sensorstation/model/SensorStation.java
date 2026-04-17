package at.qe.skeleton.sensorstation.model;

import at.qe.skeleton.measurement.model.Measurement;
import at.qe.skeleton.raspberrypi.model.DeviceStatus;
import at.qe.skeleton.raspberrypi.model.RaspberryPi;
import at.qe.skeleton.room.model.Room;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Table(name = "sensorstations")
@Entity
public class SensorStation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private DeviceStatus deviceStatus;

    private Float measurementsPerSec;

    @ManyToOne
    @JoinColumn(name = "raspberry_pi_id", nullable = false)
    private RaspberryPi raspberryPi;

    @ManyToOne
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @OneToMany(mappedBy = "sensorStation")
    private List<Measurement> measurements = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DeviceStatus getDeviceStatus() {
        return deviceStatus;
    }

    public void setDeviceStatus(DeviceStatus deviceStatus) {
        this.deviceStatus = deviceStatus;
    }

    public Float getMeasurementsPerSec() {
        return measurementsPerSec;
    }

    public void setMeasurementsPerSec(Float measurementsPerSec) {
        this.measurementsPerSec = measurementsPerSec;
    }

    public RaspberryPi getRaspberryPi() {
        return raspberryPi;
    }

    public void setRaspberryPi(RaspberryPi raspberryPi) {
        this.raspberryPi = raspberryPi;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public List<Measurement> getMeasurements() {
        return measurements;
    }
}

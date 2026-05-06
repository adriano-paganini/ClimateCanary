package at.qe.skeleton.models;

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

    private Integer measurementInterval;

    private String bleMac;

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

    public Integer getMeasurementInterval() {
        return measurementInterval;
    }

    public void setMeasurementInterval(Integer measurementInterval) {
        this.measurementInterval = measurementInterval;
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

    public String getBleMac() {
        return bleMac;
    }
    public void setBleMac(String bleMac) {
        this.bleMac = bleMac;
    }

    public List<Measurement> getMeasurements() {
        return measurements;
    }

    public void setId(Long id){
        this.id = id;
    }
}

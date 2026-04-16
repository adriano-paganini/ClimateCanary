package at.qe.skeleton.sensorstation.model;

import at.qe.skeleton.raspberrypi.model.DeviceStatus;
import at.qe.skeleton.raspberrypi.model.RaspberryPi;
import jakarta.persistence.*;

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
}

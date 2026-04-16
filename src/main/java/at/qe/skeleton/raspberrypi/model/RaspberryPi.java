package at.qe.skeleton.raspberrypi.model;

import at.qe.skeleton.sensorstation.model.SensorStation;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Table(name = "raspberrypis")
@Entity
public class RaspberryPi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String hostName;
    private String ipAddress;

    @Enumerated(EnumType.STRING)
    private DeviceStatus deviceStatus;

    @OneToMany(mappedBy = "raspberryPi")
    private List<SensorStation> sensorStations = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public DeviceStatus getDeviceStatus() {
        return deviceStatus;
    }

    public void setDeviceStatus(DeviceStatus deviceStatus) {
        this.deviceStatus = deviceStatus;
    }

    public List<SensorStation> getSensorStations() {
        return sensorStations;
    }

    public void setSensorStations(List<SensorStation> sensorStations) {
        this.sensorStations = sensorStations;
    }
}


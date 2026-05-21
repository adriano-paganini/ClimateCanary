package at.qe.skeleton.models;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Table(name = "raspberrypis")
@Entity
public class RaspberryPi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    private String hostName;
    private String ipAddress;

    @Enumerated(EnumType.STRING)
    private DeviceStatus deviceStatus;

    @OneToMany(mappedBy = "raspberryPi")
    private List<SensorStation> sensorStations = new ArrayList<>();

    @OneToOne
    @JoinColumn(name = "room_id", unique = true)
    private Room room;

    public void removeSensorStation(SensorStation sensorStation) {
        sensorStations.remove(sensorStation);
    }



}


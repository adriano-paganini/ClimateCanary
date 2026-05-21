package at.qe.skeleton.models;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Table(name = "sensorstations")
@Entity
public class SensorStation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private DeviceStatus deviceStatus;

    private Integer measurementInterval;

    private String bleMac;

    @ManyToOne
    @JoinColumn(name = "raspberry_pi_id")
    private RaspberryPi raspberryPi;

    @ManyToOne
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @OneToMany(mappedBy = "sensorStation")
    private List<Measurement> measurements = new ArrayList<>();

}

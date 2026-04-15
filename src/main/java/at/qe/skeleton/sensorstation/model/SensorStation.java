package at.qe.skeleton.sensorstation.model;

import at.qe.skeleton.raspberrypi.model.DeviceStatus;
import jakarta.persistence.*;

@Table(name = "sensorstations")
@Entity
public class SensorStation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private DeviceStatus deviceStatus;
    private Float measurementsPerSec;

}

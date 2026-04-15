package at.qe.skeleton.raspberrypi.model;

import jakarta.persistence.*;

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




}


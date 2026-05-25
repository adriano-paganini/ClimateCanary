package at.qe.skeleton.tests.repositories;

import at.qe.skeleton.models.DeviceStatus;
import at.qe.skeleton.models.RaspberryPi;
import at.qe.skeleton.repositories.RaspberryPiRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class RaspberryPiRepositoryTest {

    @Autowired
    private RaspberryPiRepository raspberryPiRepository;

    @Test
    void findAllByDecomissionedTrue_returnsOnlyDecommissionedPis() {
        RaspberryPi decommissionedPi = new RaspberryPi();
        decommissionedPi.setHostName("decommissioned-pi");
        decommissionedPi.setIpAddress("192.168.1.200");
        decommissionedPi.setDeviceStatus(DeviceStatus.DECOMMISSIONED);

        RaspberryPi onlinePi = new RaspberryPi();
        onlinePi.setHostName("online-pi");
        onlinePi.setIpAddress("192.168.1.201");
        onlinePi.setDeviceStatus(DeviceStatus.ONLINE);

        raspberryPiRepository.save(decommissionedPi);
        raspberryPiRepository.save(onlinePi);

        List<RaspberryPi> result = raspberryPiRepository.findAllByDecomissionedTrue();

        assertThat(result)
                .extracting(RaspberryPi::getHostName)
                .contains("decommissioned-pi")
                .doesNotContain("online-pi", "rpi-room1", "rpi-common1");
    }
}

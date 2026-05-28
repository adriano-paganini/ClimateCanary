package at.qe.skeleton.scheduled;

import at.qe.skeleton.dtos.RaspberryPiUpdateDTO;
import at.qe.skeleton.models.DeviceStatus;
import at.qe.skeleton.models.RaspberryPi;
import at.qe.skeleton.models.Room;
import at.qe.skeleton.services.RaspberryPiServerService;
import at.qe.skeleton.services.RaspberryPiService;
import at.qe.skeleton.services.RoomPrivacyModeService;
import at.qe.skeleton.services.RoomService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class ScheduledMethods {

    private final RoomService roomService;
    private final RaspberryPiServerService raspberryPiServerService;
    private final RaspberryPiService raspberryPiService;
    private final RoomPrivacyModeService roomPrivacyModeService;

    public ScheduledMethods(RoomService roomService,
                            RaspberryPiServerService raspberryPiServerService,
                            RaspberryPiService raspberryPiService,
                            RoomPrivacyModeService roomPrivacyModeService) {
        this.roomService = roomService;
        this.raspberryPiServerService = raspberryPiServerService;
        this.raspberryPiService = raspberryPiService;
        this.roomPrivacyModeService = roomPrivacyModeService;
    }

    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void updatePrivacyModeForAllRaspberryPis() {
        List<Room> rooms = roomService.getAll();

        for (Room room : rooms) {
            roomPrivacyModeService.updatePrivacyModeForRoom(room.getId());
        }
    }

    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void checkHeartbeat() {
        List<RaspberryPi> pis = raspberryPiService.getAllInternal();

        for (RaspberryPi pi : pis) {
            boolean isAlive = Boolean.TRUE.equals(raspberryPiServerService.getHeartbeat(pi.getId()));
            log.info("Scheduled update of heartbeat for pi {}: {}", pi.getId(), isAlive);

            DeviceStatus newStatus = isAlive ? DeviceStatus.ONLINE : DeviceStatus.OFFLINE;

            if (pi.getDeviceStatus() != newStatus) {
                RaspberryPiUpdateDTO dto = new RaspberryPiUpdateDTO(
                        null,
                        null,
                        newStatus,
                        null,
                        null
                );

                raspberryPiService.updateInternal(pi.getId(), dto);
            }
        }
    }
}
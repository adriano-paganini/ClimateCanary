package at.qe.skeleton.scheduled;

import at.qe.skeleton.dtos.RaspberryPiUpdateDTO;
import at.qe.skeleton.dtos.RoomUpdateDTO;
import at.qe.skeleton.models.*;
import at.qe.skeleton.services.*;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ScheduledMethods {

    private final AbsenceService absenceService;
    private final RoomService roomService;
    private final EmployeeProfileService employeeProfileService;
    private final RaspberryPiServerService raspberryPiServerService;
    private final RaspberryPiService raspberryPiService;

    public ScheduledMethods(AbsenceService absenceService, RoomService roomService, EmployeeProfileService employeeProfileService, RaspberryPiServerService raspberryPiServerService, RaspberryPiService raspberryPiService) {
        this.absenceService = absenceService;
        this.roomService = roomService;
        this.employeeProfileService = employeeProfileService;
        this.raspberryPiServerService = raspberryPiServerService;
        this.raspberryPiService = raspberryPiService;
    }

    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void updatePrivacyModeForAllRaspberryPis() {
        LocalDateTime now = LocalDateTime.now();

        List<Absence> allCurrentAbsences = absenceService.getByTimeframe(now, now.plusMinutes(5));

        Set<Long> absentUserIds = allCurrentAbsences.stream()
                .map(Absence::getUser)
                .map(Userx::getId)
                .collect(Collectors.toSet());

        List<Room> rooms = roomService.getAll();

        for (Room room : rooms) {
            if (room.getRoomType() == RoomType.COMMON_AREAS && room.getRaspberryPi() != null) {

                List<EmployeeProfile> assignedEmployees = employeeProfileService.getAll(room.getId(), null);

                long occupancyCount = assignedEmployees.stream()
                        .map(EmployeeProfile::getUser)
                        .map(Userx::getId)
                        .filter(userId -> !absentUserIds.contains(userId))
                        .distinct()
                        .count();

                boolean newPrivacyMode = occupancyCount < 5;
                boolean currentPrivacyMode = Boolean.TRUE.equals(room.getPrivacyMode());

                if (currentPrivacyMode != newPrivacyMode) {
                    PiRequestResult result = raspberryPiServerService.setOccupancy(
                            room.getRaspberryPi().getId(),
                            room.getId(),
                            newPrivacyMode
                    );

                    if (result == PiRequestResult.SUCCESS) {
                        roomService.update(
                                room.getId(),
                                new RoomUpdateDTO(null, null, newPrivacyMode, null, null)
                        );
                        log.info("Scheduled update of privacy mode for room {}: {}", room.getId(), newPrivacyMode);
                    }
                }
            }
        }
    }

    @Scheduled(cron = "*/30 * * * * *")
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
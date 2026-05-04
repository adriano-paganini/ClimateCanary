package at.qe.skeleton.scheduled;

import at.qe.skeleton.models.*;
import at.qe.skeleton.services.AbsenceService;
import at.qe.skeleton.services.EmployeeProfileService;
import at.qe.skeleton.services.RaspberryPiServerService;
import at.qe.skeleton.services.RoomService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class PrivacyModePeriodicUpdater {

    private final AbsenceService absenceService;
    private final RoomService roomService;
    private final EmployeeProfileService employeeProfileService;
    private final RaspberryPiServerService raspberryPiServerService;

    public PrivacyModePeriodicUpdater(AbsenceService absenceService, RoomService roomService, EmployeeProfileService employeeProfileService, RaspberryPiServerService raspberryPiServerService) {
        this.absenceService = absenceService;
        this.roomService = roomService;
        this.employeeProfileService = employeeProfileService;
        this.raspberryPiServerService = raspberryPiServerService;
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void updatePrivacyModeForAllRaspberryPis() {
        // and notify affected Raspberry Pis if the privacy mode changed.
        LocalDateTime now = LocalDateTime.now();
        List<Absence> allCurrentAbsences = absenceService.getByTimeframe(now,now.plusMinutes(5));
        Set<Room> roomsWithAbsences = roomService.getRoomsByUserIds(allCurrentAbsences.stream().map(Absence::getUser).map(Userx::getId).toList());
        for (Room room : roomsWithAbsences) {
            if (room.getRoomType() == RoomType.OFFICE){
                List<EmployeeProfile> assignedEmployees =  employeeProfileService.getAll(room.getId(),null);
                Set<Long> absentUserIds = allCurrentAbsences.stream()
                        .map(Absence::getUser)
                        .map(Userx::getId)
                        .collect(Collectors.toSet());

                long occupancyCount = assignedEmployees.stream()
                        .map(EmployeeProfile::getUser)
                        .map(Userx::getId)
                        .filter(userId -> !absentUserIds.contains(userId))
                        .count();
                if (room.getPrivacyMode() && occupancyCount >= 5){
                    raspberryPiServerService.setOccupancy(room.getRaspberryPi().getId(),room.getId(),false);
                }else if (!room.getPrivacyMode() && occupancyCount < 5){
                    raspberryPiServerService.setOccupancy(room.getRaspberryPi().getId(),room.getId(),true);
                }
            }
        }
    }
}
package at.qe.skeleton.scheduled;

import at.qe.skeleton.models.Absence;
import at.qe.skeleton.models.Room;
import at.qe.skeleton.models.RoomType;
import at.qe.skeleton.models.Userx;
import at.qe.skeleton.services.AbsenceService;
import at.qe.skeleton.services.RoomService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Component
public class PrivacyModePeriodicUpdater {

    private final AbsenceService absenceService;
    private final RoomService roomService;

    public PrivacyModePeriodicUpdater(AbsenceService absenceService, RoomService roomService) {
        this.absenceService = absenceService;
        this.roomService = roomService;
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void updatePrivacyModeForAllRaspberryPis() {
        // and notify affected Raspberry Pis if the privacy mode changed.
        LocalDateTime now = LocalDateTime.now();
        List<Absence> allCurrentAbsences = absenceService.getByTimeframe(now,now.plusMinutes(5));
        Set<Room> roomsWithAbsences = roomService.getRoomsByUserIds(allCurrentAbsences.stream().map(Absence::getUser).map(Userx::getId).toList());
        for (Room room : roomsWithAbsences) {
            if (room.getRoomType() == RoomType.OFFICE){
                //TODO:implement logic, to determine rooms, with occupation less than 5
            }
        }
    }
}
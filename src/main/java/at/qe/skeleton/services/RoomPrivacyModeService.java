package at.qe.skeleton.services;

import at.qe.skeleton.models.Absence;
import at.qe.skeleton.models.EmployeeProfile;
import at.qe.skeleton.models.Room;
import at.qe.skeleton.models.RoomType;
import at.qe.skeleton.models.Userx;
import at.qe.skeleton.repositories.EmployeeProfileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service responsible for calculating and updating room privacy mode.
 *
 * <p>
 * Privacy mode is derived from business rules based on:
 * <ul>
 *     <li>Room type (e.g. OFFICE vs COMMON_AREAS)</li>
 *     <li>Number of assigned employees</li>
 *     <li>Current employee presence (excluding absences)</li>
 * </ul>
 *
 * <p>
 * After calculation, the updated state is persisted and optionally synchronized
 * with a connected Raspberry Pi device asynchronously after transaction commit.
 */
@Slf4j
@Service
public class RoomPrivacyModeService {

    private final RoomService roomService;
    private final EmployeeProfileRepository employeeProfileRepository;
    private final AbsenceService absenceService;
    private final RoomPrivacyModePiSyncService roomPrivacyModePiSyncService;

    public RoomPrivacyModeService(RoomService roomService,
                                  EmployeeProfileRepository employeeProfileRepository,
                                  AbsenceService absenceService,
                                  RoomPrivacyModePiSyncService roomPrivacyModePiSyncService) {
        this.roomService = roomService;
        this.employeeProfileRepository = employeeProfileRepository;
        this.absenceService = absenceService;
        this.roomPrivacyModePiSyncService = roomPrivacyModePiSyncService;
    }

    /**
     * Recalculates and updates privacy mode for a given room.
     *
     * <p>
     * The new privacy mode is persisted to the database and, if a Raspberry Pi
     * is assigned to the room, the update is asynchronously synchronized after commit.
     *
     * @param roomId ID of the room to update
     */
    @Transactional
    public void updatePrivacyModeForRoom(Long roomId) {
        if (roomId == null) {
            return;
        }

        Room room = roomService.getById(roomId);

        boolean newPrivacyMode = calculatePrivacyMode(room);

        // Always save to DB.
        roomService.updatePrivacyModeInternal(room.getId(), newPrivacyMode);

        // Only contact RPi if one exists.
        // If one exists, always send the update, even if the value did not change.
        if (room.getRaspberryPi() != null) {
            syncPiAfterCommit(
                    room.getRaspberryPi().getId(),
                    room.getId(),
                    newPrivacyMode
            );
            log.info("Updated privacy mode in DB for room id={}, privacyMode={}, Raspberry Pi sync queued",
                    room.getId(), newPrivacyMode);
        } else {
            log.info("Updated privacy mode in DB for room id={}, privacyMode={}, no Raspberry Pi assigned",
                    room.getId(), newPrivacyMode);
        }
    }

    private void syncPiAfterCommit(Long piId, Long roomId, boolean privacyMode) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    roomPrivacyModePiSyncService.synchronize(piId, roomId, privacyMode);
                }
            });
            return;
        }

        roomPrivacyModePiSyncService.synchronize(piId, roomId, privacyMode);
    }

    /**
     * Calculates whether privacy mode should be enabled for a room.
     *
     * <p>
     * Rules:
     * <ul>
     *     <li>COMMON_AREAS → always false</li>
     *     <li>Non-OFFICE rooms → always false</li>
     *     <li>OFFICE rooms:
     *         <ul>
     *             <li>&lt; 5 assigned employees → privacy mode ON</li>
     *             <li>&lt; 5 currently present employees (excluding absences) → ON</li>
     *         </ul>
     *     </li>
     * </ul>
     *
     * @param room the room to evaluate
     * @return true if privacy mode should be enabled
     */
    private boolean calculatePrivacyMode(Room room) {
        if (room.getRoomType() == RoomType.COMMON_AREAS) {
            return false;
        }

        if (room.getRoomType() != RoomType.OFFICE) {
            return false;
        }

        List<EmployeeProfile> assignedEmployees =
                employeeProfileRepository.findByRoom_Id(room.getId());

        long assignedEmployeeCount = assignedEmployees.stream()
                .map(EmployeeProfile::getUser)
                .filter(java.util.Objects::nonNull)
                .map(Userx::getId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .count();

        if (assignedEmployeeCount < 5) {
            return true;
        }

        LocalDateTime now = LocalDateTime.now();

        List<Absence> currentAbsences = absenceService.getByTimeframe(now, now);

        Set<Long> absentUserIds = currentAbsences.stream()
                .map(Absence::getUser)
                .filter(java.util.Objects::nonNull)
                .map(Userx::getId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        long presentEmployeeCount = assignedEmployees.stream()
                .map(EmployeeProfile::getUser)
                .filter(java.util.Objects::nonNull)
                .map(Userx::getId)
                .filter(java.util.Objects::nonNull)
                .filter(userId -> !absentUserIds.contains(userId))
                .distinct()
                .count();

        return presentEmployeeCount < 5;
    }
}

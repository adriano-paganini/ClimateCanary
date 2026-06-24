package at.qe.skeleton.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Service responsible for synchronizing room privacy mode changes
 * with connected Raspberry Pi devices.
 *
 * <p>
 * This service acts as an async bridge between backend state changes
 * and IoT device updates, ensuring that occupancy/privacy settings
 * are propagated to Raspberry Pis without blocking the main thread.
 */
@Slf4j
@Service
public class RoomPrivacyModePiSyncService {

    private final RaspberryPiServerService raspberryPiServerService;

    public RoomPrivacyModePiSyncService(RaspberryPiServerService raspberryPiServerService) {
        this.raspberryPiServerService = raspberryPiServerService;
    }

    /**
     * Asynchronously synchronizes the privacy mode of a room with a Raspberry Pi.
     *
     * <p>
     * Sends an occupancy/privacy update to the device and logs the result.
     * Failures are logged but do not interrupt application flow.
     *
     * @param piId Raspberry Pi identifier
     * @param roomId Room identifier
     * @param privacyMode whether privacy mode is enabled or disabled
     */
    @Async
    public void synchronize(Long piId, Long roomId, boolean privacyMode) {
        PiRequestResult result = raspberryPiServerService.setOccupancy(piId, roomId, privacyMode);

        if (result != PiRequestResult.SUCCESS) {
            log.warn("Could not push privacy mode update for room {} to Raspberry Pi {}: {}",
                    roomId, piId, result);
        }

        log.info("Synchronized privacy mode for room id={}, privacyMode={}, piResult={}",
                roomId, privacyMode, result);
    }
}

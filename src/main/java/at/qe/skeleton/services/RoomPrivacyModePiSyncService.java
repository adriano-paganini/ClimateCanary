package at.qe.skeleton.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RoomPrivacyModePiSyncService {

    private final RaspberryPiServerService raspberryPiServerService;

    public RoomPrivacyModePiSyncService(RaspberryPiServerService raspberryPiServerService) {
        this.raspberryPiServerService = raspberryPiServerService;
    }

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

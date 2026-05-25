package at.qe.skeleton.services;

import at.qe.skeleton.dtos.ClimateHintDTO;
import at.qe.skeleton.dtos.ThresholdDTO;
import at.qe.skeleton.dtos.ViolationResolvedDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ThresholdPiSyncService {

    private final RaspberryPiServerService raspberryPiServerService;

    public ThresholdPiSyncService(RaspberryPiServerService raspberryPiServerService) {
        this.raspberryPiServerService = raspberryPiServerService;
    }

    @Async
    public void synchronize(
            Long thresholdId,
            Long oldPiId,
            ThresholdDTO oldThresholdDTO,
            Long newPiId,
            ThresholdDTO updatedThresholdDTO,
            List<ClimateHintDTO> climateHints,
            boolean enabled,
            List<ViolationResolvedDTO> resolvedViolations
    ) {
        if (oldPiId == null && newPiId == null) {
            log.warn("Could not synchronize threshold with id={} because neither old nor new Raspberry Pi exists", thresholdId);
            return;
        }

        if (oldPiId != null && oldThresholdDTO != null) {
            PiRequestResult deletionResult = raspberryPiServerService.deleteThresholds(
                    oldPiId,
                    List.of(oldThresholdDTO)
            );

            if (deletionResult != PiRequestResult.SUCCESS) {
                log.warn("Failed to delete old threshold with id={} on Raspberry Pi {}: result={}",
                        thresholdId, oldPiId, deletionResult);
                return;
            }

            log.info("Deleted old threshold with id={} on Raspberry Pi {}", thresholdId, oldPiId);
        }

        if (!enabled) {
            log.info("Threshold with id={} is disabled, so it will not be sent as active configuration to Raspberry Pi", thresholdId);
            resolveViolations(newPiId, resolvedViolations);
            return;
        }

        if (newPiId == null || updatedThresholdDTO == null) {
            log.warn("Could not send updated threshold with id={} because the updated room has no Raspberry Pi", thresholdId);
            return;
        }

        PiRequestResult insertionResult = raspberryPiServerService.informAboutNewThresholds(
                newPiId,
                Map.of(updatedThresholdDTO, climateHints)
        );

        if (insertionResult == PiRequestResult.SUCCESS) {
            log.info("Sent updated threshold with id={} to Raspberry Pi {}", thresholdId, newPiId);
        } else {
            log.warn("Failed to send updated threshold with id={} to Raspberry Pi {}: result={}",
                    thresholdId, newPiId, insertionResult);
        }
    }

    private void resolveViolations(Long piId, List<ViolationResolvedDTO> resolvedViolations) {
        if (piId == null || resolvedViolations.isEmpty()) {
            return;
        }

        for (ViolationResolvedDTO dto : resolvedViolations) {
            PiRequestResult result = raspberryPiServerService.resolveActiveViolation(piId, dto);
            if (result != PiRequestResult.SUCCESS) {
                log.warn("Could not notify Pi {} about disabled violation for room {} and metric {}: result={}",
                        piId, dto.roomId(), dto.metric(), result);
            }
        }
    }
}

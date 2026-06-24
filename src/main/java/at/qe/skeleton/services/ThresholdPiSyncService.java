package at.qe.skeleton.services;

import at.qe.skeleton.dtos.ClimateHintDTO;
import at.qe.skeleton.dtos.ThresholdDTO;
import at.qe.skeleton.dtos.ViolationResolvedDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Service responsible for synchronizing threshold configurations with Raspberry Pi devices.
 *
 * <p>This service ensures that threshold updates are properly propagated to the correct
 * Raspberry Pi, including handling:</p>
 * <ul>
 *     <li>Deletion of old threshold configurations</li>
 *     <li>Insertion of updated threshold configurations</li>
 *     <li>Disabling of thresholds</li>
 *     <li>Resolution of active violations when thresholds are disabled</li>
 * </ul>
 *
 * <p>All synchronization operations are executed asynchronously.</p>
 */
@Slf4j
@Service
public class ThresholdPiSyncService {

    private final RaspberryPiServerService raspberryPiServerService;

    public ThresholdPiSyncService(RaspberryPiServerService raspberryPiServerService) {
        this.raspberryPiServerService = raspberryPiServerService;
    }

    /**
     * Synchronizes threshold state between database and Raspberry Pi devices.
     *
     * <p>Workflow:</p>
     * <ul>
     *     <li>Removes old threshold configuration from previous Pi (if applicable)</li>
     *     <li>If threshold is disabled, resolves any active violations and stops</li>
     *     <li>Sends updated threshold configuration to the new Pi (if applicable)</li>
     * </ul>
     *
     * @param thresholdId ID of the threshold being synchronized
     * @param oldPiId previous Raspberry Pi ID (may be null)
     * @param oldThresholdDTO previous threshold representation (may be null)
     * @param newPiId new Raspberry Pi ID (may be null)
     * @param updatedThresholdDTO updated threshold representation (may be null)
     * @param climateHints list of climate hints associated with the threshold
     * @param enabled whether the threshold is active
     * @param resolvedViolations list of violations that should be resolved if threshold is disabled
     */
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

    /**
     * Sends violation resolution updates to a Raspberry Pi when a threshold is disabled.
     *
     * <p>Iterates over all resolved violations and notifies the Pi that they are no longer active.</p>
     *
     * @param piId Raspberry Pi ID
     * @param resolvedViolations list of resolved violation DTOs
     */
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

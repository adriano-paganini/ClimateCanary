package at.qe.skeleton.services;

import at.qe.skeleton.dtos.ThresholdCreateDTO;
import at.qe.skeleton.dtos.ThresholdDTO;
import at.qe.skeleton.dtos.ThresholdUpdateDTO;
import at.qe.skeleton.common.exceptions.ConflictException;
import at.qe.skeleton.common.exceptions.NotFoundException;
import at.qe.skeleton.mappers.ThresholdMapper;
import at.qe.skeleton.models.ClimateHint;
import at.qe.skeleton.models.Metric;
import at.qe.skeleton.models.Threshold;
import at.qe.skeleton.repositories.ClimateHintRepository;
import at.qe.skeleton.repositories.ThresholdRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashSet;
import java.util.List;

@Slf4j
@Service
public class ThresholdService {

    private final ThresholdRepository thresholdRepository;
    private final RoomService roomService;
    private final ClimateHintRepository climateHintRepository;
    private final RaspberryPiServerService raspberryPiServerService;
    private final ThresholdMapper thresholdMapper;

    public ThresholdService(ThresholdRepository thresholdRepository,
                            RoomService roomService,
                            ClimateHintRepository climateHintRepository, RaspberryPiServerService raspberryPiServerService, ThresholdMapper thresholdMapper) {
        this.thresholdRepository = thresholdRepository;
        this.roomService = roomService;
        this.climateHintRepository = climateHintRepository;
        this.raspberryPiServerService = raspberryPiServerService;
        this.thresholdMapper = thresholdMapper;
    }

    public List<Threshold> getAll(Long roomId, Metric metric){

        if (roomId != null && metric != null) {
            return thresholdRepository.findByRoom_IdAndMetric(roomId, metric);
        }

        if (roomId != null) return thresholdRepository.findByRoom_Id(roomId);
        if (metric != null) return thresholdRepository.findByMetric(metric);

        return thresholdRepository.findAll();
    }

    public Threshold getThresholdById(Long id){
        return thresholdRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Threshold with id " + id + " not found"));
    }

    public Threshold create(ThresholdCreateDTO dto) {
        Threshold entity = new Threshold();
        entity.setMetric(dto.metric());
        entity.setBoundValue(dto.boundValue());
        entity.setThresholdType(dto.thresholdType());
        entity.setEnabled(true);
        entity.setRoom(roomService.getById(dto.roomId()));

        if (dto.climateHintIds() != null) {
            List<ClimateHint> hints = climateHintRepository.findAllById(dto.climateHintIds());
            entity.setClimateHints(new HashSet<>(hints));
        }

        Threshold savedThreshold = thresholdRepository.save(entity);

        log.info("Created threshold with id={}", savedThreshold.getId());
        log.debug("Created threshold details: id={}, metric={}, boundValue={}, thresholdType={}, enabled={}, roomId={}, climateHintIds={}", savedThreshold.getId(), savedThreshold.getMetric(), savedThreshold.getBoundValue(), savedThreshold.getThresholdType(), savedThreshold.isEnabled(), savedThreshold.getRoom() != null ? savedThreshold.getRoom().getId() : null, dto.climateHintIds());

        return savedThreshold;
    }

    @Transactional
    public Threshold update(Long id, ThresholdUpdateDTO dto) {
        Threshold entity = getThresholdById(id);

        ThresholdDTO oldThresholdDTO = thresholdMapper.mapTo(entity);
        Long oldPiId = getRaspberryPiIdOrNull(entity);

        StringBuilder debugInfo = new StringBuilder("Updated threshold details:")
                .append(" id=").append(id);

        if (dto.metric() != null) {
            entity.setMetric(dto.metric());
            debugInfo.append(", metric=").append(dto.metric());
        }

        if (dto.boundValue() != null) {
            entity.setBoundValue(dto.boundValue());
            debugInfo.append(", boundValue=").append(dto.boundValue());
        }

        if (dto.thresholdType() != null) {
            entity.setThresholdType(dto.thresholdType());
            debugInfo.append(", thresholdType=").append(dto.thresholdType());
        }

        if (dto.enabled() != null) {
            entity.setEnabled(dto.enabled());
            debugInfo.append(", enabled=").append(dto.enabled());
        }

        if (dto.roomId() != null) {
            entity.setRoom(roomService.getById(dto.roomId()));
            debugInfo.append(", roomId=").append(dto.roomId());
        }

        if (dto.climateHintIds() != null) {
            List<ClimateHint> hints = climateHintRepository.findAllById(dto.climateHintIds());
            entity.getClimateHints().clear();
            entity.getClimateHints().addAll(hints);
            debugInfo.append(", climateHintIds=").append(dto.climateHintIds());
        }

        Threshold updatedThreshold = thresholdRepository.save(entity);

        synchronizeThresholdWithRaspberryPi(id, oldPiId, oldThresholdDTO, updatedThreshold);

        log.info("Updated threshold with id={}", id);
        log.debug(debugInfo.toString());

        return updatedThreshold;
    }

    private void synchronizeThresholdWithRaspberryPi(
            Long thresholdId,
            Long oldPiId,
            ThresholdDTO oldThresholdDTO,
            Threshold updatedThreshold
    ) {
        Long newPiId = getRaspberryPiIdOrNull(updatedThreshold);

        if (oldPiId == null && newPiId == null) {
            log.warn("Could not synchronize threshold with id={} because neither old nor new Raspberry Pi exists", thresholdId);
            return;
        }

        if (oldPiId != null) {
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

        if (!Boolean.TRUE.equals(updatedThreshold.isEnabled())) {
            log.info("Threshold with id={} is disabled, so it will not be sent as active configuration to Raspberry Pi", thresholdId);
            return;
        }

        if (newPiId == null) {
            log.warn("Could not send updated threshold with id={} because the updated room has no Raspberry Pi", thresholdId);
            return;
        }

        ThresholdDTO updatedThresholdDTO = thresholdMapper.mapTo(updatedThreshold);

        PiRequestResult insertionResult = raspberryPiServerService.informAboutNewThresholds(
                newPiId,
                List.of(updatedThresholdDTO)
        );

        if (insertionResult == PiRequestResult.SUCCESS) {
            log.info("Sent updated threshold with id={} to Raspberry Pi {}", thresholdId, newPiId);
        } else {
            log.warn("Failed to send updated threshold with id={} to Raspberry Pi {}: result={}",
                    thresholdId, newPiId, insertionResult);
        }
    }

    private Long getRaspberryPiIdOrNull(Threshold threshold) {
        if (threshold == null || threshold.getRoom() == null || threshold.getRoom().getRaspberryPi() == null) {
            return null;
        }
        return threshold.getRoom().getRaspberryPi().getId();
    }

    @Transactional
    public void delete(Long id) {
        Threshold entity = getThresholdById(id);

        if (!entity.getViolations().isEmpty()) {
            throw new ConflictException("Threshold cannot be deleted because it has violations");
        }

        Long piId = getRaspberryPiIdOrNull(entity);
        ThresholdDTO thresholdDTO = thresholdMapper.mapTo(entity);

        if (piId != null) {
            PiRequestResult deletionResult = raspberryPiServerService.deleteThresholds(
                    piId,
                    List.of(thresholdDTO)
            );

            if (deletionResult != PiRequestResult.SUCCESS) {
                log.warn("Failed to delete threshold with id={} on Raspberry Pi {}: result={}",
                        id, piId, deletionResult);
                throw new IllegalStateException("Threshold could not be deleted on Raspberry Pi");
            }

            log.info("Deleted threshold with id={} on Raspberry Pi {}", id, piId);
        } else {
            log.warn("Deleting threshold with id={} from database, but no Raspberry Pi is assigned to its room", id);
        }

        entity.getClimateHints().clear();
        thresholdRepository.delete(entity);

        log.info("Deleted threshold with id={}", id);
        log.debug("Cleared climate hint associations before deleting threshold id={}", id);
    }
}
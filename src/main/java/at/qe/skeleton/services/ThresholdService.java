package at.qe.skeleton.services;

import at.qe.skeleton.dtos.ClimateHintDTO;
import at.qe.skeleton.dtos.ThresholdCreateDTO;
import at.qe.skeleton.dtos.ThresholdDTO;
import at.qe.skeleton.dtos.ThresholdUpdateDTO;
import at.qe.skeleton.dtos.ViolationResolvedDTO;
import at.qe.skeleton.common.exceptions.ConflictException;
import at.qe.skeleton.common.exceptions.NotFoundException;
import at.qe.skeleton.mappers.ClimateHintMapper;
import at.qe.skeleton.mappers.ThresholdMapper;
import at.qe.skeleton.models.ClimateHint;
import at.qe.skeleton.models.Metric;
import at.qe.skeleton.models.Threshold;
import at.qe.skeleton.models.ThresholdViolation;
import at.qe.skeleton.models.ThresholdType;
import at.qe.skeleton.models.ViolationStatus;
import at.qe.skeleton.repositories.ClimateHintRepository;
import at.qe.skeleton.repositories.ThresholdRepository;
import at.qe.skeleton.repositories.ThresholdViolationRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

@Slf4j
@Service
public class ThresholdService {

    private final ThresholdRepository thresholdRepository;
    private final ThresholdViolationRepository thresholdViolationRepository;
    private final RoomService roomService;
    private final ClimateHintRepository climateHintRepository;
    private final RaspberryPiServerService raspberryPiServerService;
    private final ThresholdPiSyncService thresholdPiSyncService;
    private final ThresholdMapper thresholdMapper;
    private final ClimateHintMapper climateHintMapper;

    public ThresholdService(ThresholdRepository thresholdRepository,
                            ThresholdViolationRepository thresholdViolationRepository,
                            RoomService roomService,
                            ClimateHintRepository climateHintRepository,
                            RaspberryPiServerService raspberryPiServerService,
                            ThresholdPiSyncService thresholdPiSyncService,
                            ThresholdMapper thresholdMapper,
                            ClimateHintMapper climateHintMapper) {
        this.thresholdRepository = thresholdRepository;
        this.thresholdViolationRepository = thresholdViolationRepository;
        this.roomService = roomService;
        this.climateHintRepository = climateHintRepository;
        this.raspberryPiServerService = raspberryPiServerService;
        this.thresholdPiSyncService = thresholdPiSyncService;
        this.thresholdMapper = thresholdMapper;
        this.climateHintMapper = climateHintMapper;
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

    @Transactional
    public Threshold create(ThresholdCreateDTO dto) {
        if (thresholdRepository.existsByRoomIdAndMetricAndThresholdType(
                dto.roomId(),
                dto.metric(),
                dto.thresholdType()
        )) {
            throw duplicateThresholdException(dto.roomId(), dto.metric(), dto.thresholdType());
        }

        Threshold entity = new Threshold();
        entity.setMetric(dto.metric());
        entity.setBoundValue(dto.boundValue());
        entity.setThresholdType(dto.thresholdType());
        entity.setEnabled(true);
        entity.setRoom(roomService.getById(dto.roomId()));

        if (dto.climateHintIds() != null) {
            List<ClimateHint> hints = climateHintRepository.findAllById(dto.climateHintIds());
            replaceClimateHints(entity, hints);
        }

        Threshold savedThreshold = thresholdRepository.save(entity);

        log.info("Created threshold with id={}", savedThreshold.getId());
        log.debug("Created threshold details: id={}, metric={}, boundValue={}, thresholdType={}, enabled={}, roomId={}, climateHintIds={}", savedThreshold.getId(), savedThreshold.getMetric(), savedThreshold.getBoundValue(), savedThreshold.getThresholdType(), savedThreshold.isEnabled(), savedThreshold.getRoom() != null ? savedThreshold.getRoom().getId() : null, dto.climateHintIds());

        enqueueThresholdSync(savedThreshold.getId(), null, null, savedThreshold);

        return savedThreshold;
    }

    @Transactional
    public Threshold update(Long id, ThresholdUpdateDTO dto) {
        Threshold entity = getThresholdById(id);

        Long effectiveRoomId = dto.roomId() != null ? dto.roomId() : entity.getRoom().getId();
        Metric effectiveMetric = dto.metric() != null ? dto.metric() : entity.getMetric();
        var effectiveThresholdType = dto.thresholdType() != null ? dto.thresholdType() : entity.getThresholdType();

        if (thresholdRepository.existsByRoomIdAndMetricAndThresholdTypeAndIdNot(
                effectiveRoomId,
                effectiveMetric,
                effectiveThresholdType,
                id
        )) {
            throw duplicateThresholdException(effectiveRoomId, effectiveMetric, effectiveThresholdType);
        }

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
            replaceClimateHints(entity, hints);
            debugInfo.append(", climateHintIds=").append(dto.climateHintIds());
        }

        Threshold updatedThreshold = thresholdRepository.save(entity);

        enqueueThresholdSync(id, oldPiId, oldThresholdDTO, updatedThreshold);

        log.info("Updated threshold with id={}", id);
        log.debug(debugInfo.toString());

        return updatedThreshold;
    }

    private void enqueueThresholdSync(
            Long thresholdId,
            Long oldPiId,
            ThresholdDTO oldThresholdDTO,
            Threshold updatedThreshold
    ) {
        Long newPiId = getRaspberryPiIdOrNull(updatedThreshold);
        ThresholdDTO updatedThresholdDTO = thresholdMapper.mapTo(updatedThreshold);
        List<ClimateHintDTO> climateHints = updatedThreshold.getClimateHints().stream()
                .map(climateHintMapper::mapTo)
                .toList();
        List<ViolationResolvedDTO> resolvedViolations = updatedThreshold.isEnabled()
                ? List.of()
                : disableActiveViolations(thresholdId, updatedThreshold);

        Runnable syncTask = () -> thresholdPiSyncService.synchronize(
                thresholdId, oldPiId, oldThresholdDTO, newPiId, updatedThresholdDTO,
                climateHints, updatedThreshold.isEnabled(), resolvedViolations);

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    syncTask.run();
                }
            });
        } else {
            syncTask.run();
        }
    }

    private List<ViolationResolvedDTO> disableActiveViolations(Long thresholdId, Threshold threshold) {
        List<ThresholdViolation> active = thresholdViolationRepository
                .findByThreshold_IdAndViolationStatus(thresholdId, ViolationStatus.ACTIVE);

        if (active.isEmpty()) {
            return List.of();
        }

        LocalDateTime now = LocalDateTime.now();
        String endTimestamp = now.format(DateTimeFormatter.ISO_DATE_TIME);
        java.util.ArrayList<ViolationResolvedDTO> resolvedViolations = new java.util.ArrayList<>();

        for (ThresholdViolation v : active) {
            v.setViolationStatus(ViolationStatus.DISABLED);
            v.setEndTime(now);
            thresholdViolationRepository.save(v);
            log.info("Set threshold violation id={} to DISABLED (threshold id={} disabled)", v.getId(), thresholdId);

            if (v.getRoom() != null) {
                resolvedViolations.add(new ViolationResolvedDTO(v.getMetric(), v.getRoom().getId(), endTimestamp));
            }
        }

        log.info("Disabled {} active violation(s) for threshold id={}", active.size(), thresholdId);
        return resolvedViolations;
    }

    private Long getRaspberryPiIdOrNull(Threshold threshold) {
        if (threshold == null || threshold.getRoom() == null || threshold.getRoom().getRaspberryPi() == null) {
            return null;
        }
        return threshold.getRoom().getRaspberryPi().getId();
    }

    private void replaceClimateHints(Threshold threshold, Collection<ClimateHint> newHints) {
        clearClimateHints(threshold);
        for (ClimateHint hint : newHints) {
            threshold.getClimateHints().add(hint);
            hint.getThresholds().add(threshold);
        }
    }

    private void clearClimateHints(Threshold threshold) {
        for (ClimateHint hint : new HashSet<>(threshold.getClimateHints())) {
            hint.getThresholds().remove(threshold);
        }
        threshold.getClimateHints().clear();
    }

    private ConflictException duplicateThresholdException(Long roomId, Metric metric, ThresholdType thresholdType) {
        return new ConflictException(
                "Threshold already exists for room " + roomId
                        + " and metric " + metric
                        + " and threshold type " + thresholdType
        );
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

        clearClimateHints(entity);
        thresholdRepository.delete(entity);

        log.info("Deleted threshold with id={}", id);
        log.debug("Cleared climate hint associations before deleting threshold id={}", id);
    }
}

package at.qe.skeleton.services;

import at.qe.skeleton.common.exceptions.NotFoundException;
import at.qe.skeleton.dtos.ThresholdViolationCreateDTO;
import at.qe.skeleton.dtos.ThresholdViolationUpdateDTO;
import at.qe.skeleton.dtos.ViolationActiveDTO;
import at.qe.skeleton.dtos.ViolationResolvedDTO;
import at.qe.skeleton.mappers.ThresholdViolationCreateMapper;
import at.qe.skeleton.models.*;
import at.qe.skeleton.repositories.ThresholdRepository;
import at.qe.skeleton.repositories.ThresholdViolationRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class ThresholdViolationService {

    private final ThresholdViolationRepository thresholdViolationRepository;
    private final RoomService roomService;
    private final ThresholdService thresholdService;
    private final ThresholdViolationCreateMapper thresholdViolationCreateMapper;
    private final ThresholdRepository thresholdRepository;
    private final MeasurementService measurementService;
    private final RaspberryPiService raspberryPiService;
    private final RaspberryPiServerService raspberryPiServerService;


    public ThresholdViolationService(ThresholdViolationRepository thresholdViolationRepository,
                                     RoomService roomService,
                                     ThresholdService thresholdService, ThresholdViolationCreateMapper thresholdViolationCreateMapper, ThresholdRepository thresholdRepository, MeasurementService measurementService, RaspberryPiService raspberryPiService, RaspberryPiServerService raspberryPiServerService) {
        this.thresholdViolationRepository = thresholdViolationRepository;
        this.roomService = roomService;
        this.thresholdService = thresholdService;
        this.thresholdViolationCreateMapper = thresholdViolationCreateMapper;
        this.thresholdRepository = thresholdRepository;
        this.measurementService = measurementService;
        this.raspberryPiService = raspberryPiService;
        this.raspberryPiServerService = raspberryPiServerService;
    }

    public List<ThresholdViolation> findAll(
            ViolationStatus status,
            Long roomId,
            Long departmentId
    ) {
        if (status == null && roomId == null && departmentId == null) {
            return thresholdViolationRepository.findAll();
        }

        return thresholdViolationRepository.search(status, roomId, departmentId);
    }

    public ThresholdViolation findById(Long id) {
        return thresholdViolationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("There is no such threshold violation with id: " + id));
    }

    public ThresholdViolation create(ThresholdViolationCreateDTO dto) {
        ThresholdViolation entity = thresholdViolationCreateMapper.mapFrom(dto);
        ThresholdViolation savedViolation = thresholdViolationRepository.save(entity);

        log.info("Created threshold violation with id={}", savedViolation.getId());
        log.debug("Created threshold violation details: id={}, metric={}, value={}, startTime={}, thresholdId={}, roomId={}, violationStatus={}",
                savedViolation.getId(),
                savedViolation.getMetric(),
                savedViolation.getValue(),
                savedViolation.getStartTime(),
                savedViolation.getThreshold() != null ? savedViolation.getThreshold().getId() : null,
                savedViolation.getRoom() != null ? savedViolation.getRoom().getId() : null,
                savedViolation.getViolationStatus());

        return savedViolation;
    }



    public ThresholdViolation create(Long piId, ViolationActiveDTO dto) {
        LocalDateTime time = LocalDateTime.parse(dto.startTime(), DateTimeFormatter.ISO_DATE_TIME);

        log.info(
                "Creating threshold violation for raspberryPiId={}, roomId={}, metric={}",
                piId,
                dto.roomId(),
                dto.metric()
        );

        RaspberryPi raspberryPi = raspberryPiService.getByIdInternal(piId);
        Long roomId = raspberryPi.getRoom().getId();

        if (!Objects.equals(roomId, dto.roomId())) {
            log.warn(
                    "Failed to create threshold violation: raspberryPiId={} is in roomId={}, but dto roomId={}",
                    piId,
                    roomId,
                    dto.roomId()
            );
            throw new NotFoundException("Raspberry Pi is not in room " + dto.roomId());
        }

        Long thresholdId = determineThreshold(roomId, dto.metric(), dto.avgValue()).getId();

        List<Long> measurementIds = measurementService
                .getFiltered(roomId, dto.metric(), time, LocalDateTime.now())
                .stream()
                .map(Measurement::getId)
                .toList();

        log.debug(
                "Creating threshold violation details: raspberryPiId={}, roomId={}, metric={}, avgValue={}, thresholdId={}, startTime={}, measurementIds={}",
                piId,
                roomId,
                dto.metric(),
                dto.avgValue(),
                thresholdId,
                time,
                measurementIds
        );

        ThresholdViolationCreateDTO createDTO = new ThresholdViolationCreateDTO(
                dto.metric(),
                dto.avgValue(),
                time,
                thresholdId,
                roomId,
                measurementIds
        );

        ThresholdViolation entity = thresholdViolationCreateMapper.mapFrom(createDTO);
        ThresholdViolation savedViolation = thresholdViolationRepository.save(entity);

        log.info(
                "Created threshold violation with id={} for roomId={}, metric={}, thresholdId={}",
                savedViolation.getId(),
                roomId,
                dto.metric(),
                thresholdId
        );

        return savedViolation;
    }

    @Transactional
    public ThresholdViolation update(Long id, ThresholdViolationUpdateDTO dto) {
        ThresholdViolation entity = findById(id);

        ViolationStatus previousStatus = entity.getViolationStatus();

        StringBuilder debugInfo = new StringBuilder("Updated threshold violation details:")
                .append(" id=").append(id);

        if (dto.metric() != null) {
            entity.setMetric(dto.metric());
            debugInfo.append(", metric=").append(dto.metric());
        }

        if (dto.value() != null) {
            entity.setValue(dto.value());
            debugInfo.append(", value=").append(dto.value());
        }

        if (dto.startTime() != null) {
            entity.setStartTime(dto.startTime());
            debugInfo.append(", startTime=").append(dto.startTime());
        }

        if (dto.endTime() != null) {
            entity.setEndTime(dto.endTime());
            debugInfo.append(", endTime=").append(dto.endTime());
        }

        if (dto.thresholdId() != null) {
            entity.setThreshold(thresholdService.getThresholdById(dto.thresholdId()));
            debugInfo.append(", thresholdId=").append(dto.thresholdId());
        }

        if (dto.roomId() != null) {
            entity.setRoom(roomService.getById(dto.roomId()));
            debugInfo.append(", roomId=").append(dto.roomId());
        }

        boolean shouldNotifyPiAboutResolution = false;

        if (dto.violationStatus() != null) {
            entity.setViolationStatus(dto.violationStatus());
            debugInfo.append(", violationStatus=").append(dto.violationStatus());

            shouldNotifyPiAboutResolution =
                    previousStatus != ViolationStatus.RESOLVED
                            && dto.violationStatus() == ViolationStatus.RESOLVED;

            if (shouldNotifyPiAboutResolution && entity.getEndTime() == null) {
                entity.setEndTime(LocalDateTime.now());
                debugInfo.append(", endTime=").append(entity.getEndTime());
            }
        }

        ThresholdViolation updatedViolation = thresholdViolationRepository.save(entity);

        if (shouldNotifyPiAboutResolution) {
            notifyRaspberryPiAboutResolvedViolation(updatedViolation);
        }

        log.info("Updated threshold violation with id={}", id);
        log.debug(debugInfo.toString());

        return updatedViolation;
    }

    private void notifyRaspberryPiAboutResolvedViolation(ThresholdViolation violation) {
        Long piId = getRaspberryPiIdOrNull(violation);

        if (piId == null) {
            log.warn("Could not notify Raspberry Pi about resolved violation with id={} because no Raspberry Pi is assigned",
                    violation.getId());
            throw new IllegalStateException("Cannot resolve violation on Raspberry Pi because no Raspberry Pi is assigned");
        }

        if (violation.getRoom() == null) {
            throw new IllegalStateException("Cannot resolve violation on Raspberry Pi because violation has no room");
        }
        String endTimestamp = violation.getEndTime().format(DateTimeFormatter.ISO_DATE_TIME);

        PiRequestResult result = raspberryPiServerService.resolveActiveViolation(
                piId,
                new ViolationResolvedDTO(
                        violation.getMetric(),
                        violation.getRoom().getId(),
                        endTimestamp
                )
        );

        if (result != PiRequestResult.SUCCESS) {
            log.warn("Failed to resolve violation with id={} on Raspberry Pi {}: result={}",
                    violation.getId(), piId, result);
            throw new IllegalStateException("Violation could not be resolved on Raspberry Pi");
        }

        log.info("Resolved violation with id={} on Raspberry Pi {}", violation.getId(), piId);
    }

    private Long getRaspberryPiIdOrNull(ThresholdViolation thresholdViolation) {
        if (thresholdViolation == null || thresholdViolation.getRoom() == null || thresholdViolation.getRoom().getRaspberryPi() == null) {
            return null;
        }
        return thresholdViolation.getRoom().getRaspberryPi().getId();
    }

    public ThresholdViolation update(Long piId, ViolationResolvedDTO dto) {
        log.info(
                "Resolving threshold violation for raspberryPiId={}, roomId={}, metric={}",
                piId,
                dto.roomId(),
                dto.metric()
        );

        RaspberryPi raspberryPi = raspberryPiService.getByIdInternal(piId);
        Long roomId = raspberryPi.getRoom().getId();

        if (!Objects.equals(roomId, dto.roomId())) {
            log.warn(
                    "Failed to resolve threshold violation: raspberryPiId={} is in roomId={}, but dto roomId={}",
                    piId,
                    roomId,
                    dto.roomId()
            );
            throw new NotFoundException("Raspberry Pi is not in room " + dto.roomId());
        }

        LocalDateTime time = LocalDateTime.now();

        ThresholdViolation violation =
                thresholdViolationRepository.findByRoomIdAndMetricAndViolationStatus(
                                roomId,
                                dto.metric(),
                                ViolationStatus.ACTIVE
                        )
                        .orElseThrow(() -> {
                            log.warn(
                                    "No active threshold violation found for roomId={} and metric={}",
                                    roomId,
                                    dto.metric()
                            );
                            return new NotFoundException(
                                    "No active threshold violation for room " + roomId + " and metric " + dto.metric()
                            );
                        });

        List<Measurement> measurements = measurementService.getFiltered(
                roomId,
                dto.metric(),
                violation.getStartTime(),
                time
        );

        log.debug(
                "Resolving threshold violation details: violationId={}, roomId={}, metric={}, startTime={}, endTime={}, measurementCount={}",
                violation.getId(),
                roomId,
                dto.metric(),
                violation.getStartTime(),
                time,
                measurements.size()
        );

        violation.setEndTime(time);
        violation.setViolationStatus(ViolationStatus.RESOLVED);
        violation.setMeasurements(measurements);

        ThresholdViolation savedViolation = thresholdViolationRepository.save(violation);

        log.info(
                "Resolved threshold violation with id={} for roomId={} and metric={}",
                savedViolation.getId(),
                roomId,
                dto.metric()
        );

        return savedViolation;
    }

    private Threshold determineThreshold(Long roomId, Metric metric, Float avgValue) {
        List<Threshold> thresholds = thresholdRepository.findByRoom_IdAndMetric(roomId, metric);

        if (thresholds.isEmpty() || thresholds.size() > 2) {
            throw new NotFoundException("Cannot specify threshold choice for room " + roomId + " and metric " + metric);
        }

        if (thresholds.size() == 1) {
            return thresholds.getFirst();
        }

        return thresholds.stream()
                .filter(threshold -> matchesThreshold(threshold, avgValue))
                .findFirst()
                .orElse(null);
    }

    private boolean matchesThreshold(Threshold threshold, Float avgValue) {
        return switch (threshold.getThresholdType()) {
            case UPPER -> avgValue >= threshold.getBoundValue();
            case LOWER -> avgValue <= threshold.getBoundValue();
        };
    }

    public void delete(Long id) {
        thresholdViolationRepository.deleteById(id);
        log.info("Deleted threshold violation with id={}", id);
    }
}

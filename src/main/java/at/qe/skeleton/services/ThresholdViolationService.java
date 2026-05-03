package at.qe.skeleton.services;

import at.qe.skeleton.common.exceptions.NotFoundException;
import at.qe.skeleton.dtos.ThresholdViolationCreateDTO;
import at.qe.skeleton.dtos.ThresholdViolationUpdateDTO;
import at.qe.skeleton.dtos.ViolationActiveDTO;
import at.qe.skeleton.dtos.ViolationResolvedDTO;
import at.qe.skeleton.mappers.ThresholdViolationCreateMapper;
import at.qe.skeleton.models.*;
import at.qe.skeleton.repositories.MeasurementRepository;
import at.qe.skeleton.repositories.ThresholdRepository;
import at.qe.skeleton.repositories.ThresholdViolationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

    public ThresholdViolationService(ThresholdViolationRepository thresholdViolationRepository,
                                     RoomService roomService,
                                     ThresholdService thresholdService, ThresholdViolationCreateMapper thresholdViolationCreateMapper, ThresholdRepository thresholdRepository, MeasurementService measurementService, RaspberryPiService raspberryPiService) {
        this.thresholdViolationRepository = thresholdViolationRepository;
        this.roomService = roomService;
        this.thresholdService = thresholdService;
        this.thresholdViolationCreateMapper = thresholdViolationCreateMapper;
        this.thresholdRepository = thresholdRepository;
        this.measurementService = measurementService;
        this.raspberryPiService = raspberryPiService;
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
        //TODO: parse dto time-string
        LocalDateTime time = LocalDateTime.now();
        RaspberryPi raspberryPi = raspberryPiService.getById(piId);
        Long roomId = raspberryPi.getRoom().getId();
        if (!Objects.equals(roomId, dto.roomId())) throw new NotFoundException("Raspberry Pi is not in room " + dto.roomId());

        Long thresholdId = determineThreshold(roomId,dto.metric(),dto.avgValue()).getId();
        List<Long> measurementIds = measurementService.getFiltered(roomId,dto.metric(),time,LocalDateTime.now()).stream().map(Measurement::getId).toList();

        ThresholdViolationCreateDTO createDTO = new ThresholdViolationCreateDTO(dto.metric(),dto.avgValue(),time,thresholdId,roomId,measurementIds);
        ThresholdViolation entity = thresholdViolationCreateMapper.mapFrom(createDTO);
        return thresholdViolationRepository.save(entity);
    }

    public ThresholdViolation update(Long id, ThresholdViolationUpdateDTO dto) {
        ThresholdViolation entity = findById(id);

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

        if (dto.violationStatus() != null) {
            entity.setViolationStatus(dto.violationStatus());
            debugInfo.append(", violationStatus=").append(dto.violationStatus());
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

        ThresholdViolation updatedViolation = thresholdViolationRepository.save(entity);

        log.info("Updated threshold violation with id={}", id);
        log.debug(debugInfo.toString());

        return updatedViolation;
    }

    public ThresholdViolation update(Long piId, ViolationResolvedDTO dto){
        RaspberryPi raspberryPi = raspberryPiService.getById(piId);
        Long roomId = raspberryPi.getRoom().getId();
        if (!Objects.equals(roomId, dto.roomId())) throw new NotFoundException("Raspberry Pi is not in room " + dto.roomId());
        LocalDateTime time = LocalDateTime.now();

        ThresholdViolation violation =
                thresholdViolationRepository.findByRoomIdAndMetricAndViolationStatus(roomId,dto.metric(),ViolationStatus.ACTIVE)
                        .orElseThrow(() -> new NotFoundException("No active threshold violation for room " + roomId + " and metric " + dto.metric()));

        List<Measurement> measurements = measurementService.getFiltered(roomId,dto.metric(),violation.getStartTime(),time);

        violation.setEndTime(time);
        violation.setViolationStatus(ViolationStatus.RESOLVED);
        violation.setMeasurements(measurements);
        return thresholdViolationRepository.save(violation);
    }

    private Threshold determineThreshold(Long roomId, Metric metric, Float avgValue) {
        List<Threshold> thresholds = thresholdRepository.findByRoom_IdAndMetric(roomId, metric);
        Threshold relevantThreshold = null;
        if (thresholds.isEmpty() || thresholds.size()>2) throw new NotFoundException("Cannot specify threshold choice for room " + roomId + " and metric " + metric);
        else if(thresholds.size()==1){
            relevantThreshold = thresholds.getFirst();
        }else{
            for (Threshold threshold : thresholds) {
                switch (threshold.getThresholdType()) {
                    case ThresholdType.UPPER:
                    {
                        if (avgValue >= threshold.getBoundValue()){
                            relevantThreshold = threshold;
                        }
                        break;
                    }
                    case ThresholdType.LOWER:
                    {
                        if (avgValue <= threshold.getBoundValue()){
                            relevantThreshold = threshold;
                        }
                        break;
                    }
                }
            }
        }
        return relevantThreshold;
    }

    public void delete(Long id) {
        thresholdViolationRepository.deleteById(id);
        log.info("Deleted threshold violation with id={}", id);
    }
}

package at.qe.skeleton.violation.mapper;

import at.qe.skeleton.violation.dto.ThresholdViolationCreateDTO;
import at.qe.skeleton.common.exceptions.RoomNotFoundException;
import at.qe.skeleton.common.exceptions.ThresholdNotFoundException;
import at.qe.skeleton.common.DTOMapper;
import at.qe.skeleton.violation.model.ThresholdViolation;
import at.qe.skeleton.violation.model.ViolationStatus;
import at.qe.skeleton.room.repository.RoomRepository;
import at.qe.skeleton.threshold.repository.ThresholdRepository;
import org.springframework.stereotype.Service;

@Service
public class ThresholdViolationCreateMapper implements DTOMapper<ThresholdViolation, ThresholdViolationCreateDTO> {

    private final ThresholdRepository thresholdRepository;
    private final RoomRepository roomRepository;

    public ThresholdViolationCreateMapper(
            ThresholdRepository thresholdRepository,
            RoomRepository roomRepository) {
        this.thresholdRepository = thresholdRepository;
        this.roomRepository = roomRepository;
    }

    @Override
    public ThresholdViolation mapFrom(ThresholdViolationCreateDTO dto) {
        ThresholdViolation entity = new ThresholdViolation();
        entity.setMetric(dto.metric());
        entity.setValue(dto.value());
        entity.setStartTime(dto.startTime());

        entity.setThreshold(thresholdRepository.findById(dto.thresholdId())
                        .orElseThrow(() ->
                                new ThresholdNotFoundException("Threshold with id " + dto.thresholdId() + " not found"))
        );

        entity.setRoom(roomRepository.findById(dto.roomId())
                        .orElseThrow(() -> new RoomNotFoundException("Room with id " + dto.roomId() + " not found"))
        );

        entity.setViolationStatus(ViolationStatus.ACTIVE);
        return entity;
    }

    @Override
    public ThresholdViolationCreateDTO mapTo(ThresholdViolation entity) {
        throw new UnsupportedOperationException();
    }
}

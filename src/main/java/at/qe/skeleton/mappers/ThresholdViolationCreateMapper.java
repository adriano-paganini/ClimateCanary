package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.ThresholdViolationCreateDTO;
import at.qe.skeleton.exceptions.RoomNotFoundException;
import at.qe.skeleton.exceptions.ThresholdNotFoundException;
import at.qe.skeleton.model.ThresholdViolation;
import at.qe.skeleton.model.ViolationStatus;
import at.qe.skeleton.repositories.RoomRepository;
import at.qe.skeleton.repositories.ThresholdRepository;
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

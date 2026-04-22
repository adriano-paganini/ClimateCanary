package at.qe.skeleton.mappers;

import at.qe.skeleton.common.DTOMapper;
import at.qe.skeleton.dtos.ThresholdCreateDTO;
import at.qe.skeleton.common.exceptions.RoomNotFoundException;
import at.qe.skeleton.models.Threshold;
import at.qe.skeleton.repositories.ClimateHintRepository;
import at.qe.skeleton.repositories.RoomRepository;
import org.springframework.stereotype.Service;

import java.util.HashSet;

@Service
public class ThresholdCreateMapper implements DTOMapper<Threshold, ThresholdCreateDTO> {

    private final RoomRepository roomRepository;
    private final ClimateHintRepository hintRepository;

    public ThresholdCreateMapper(RoomRepository roomRepository,
                                 ClimateHintRepository hintRepository) {
        this.roomRepository = roomRepository;
        this.hintRepository = hintRepository;
    }

    @Override
    public Threshold mapFrom(ThresholdCreateDTO dto) {
        Threshold entity = new Threshold();
        entity.setMetric(dto.metric());
        entity.setBoundValue(dto.boundValue());
        entity.setThresholdType(dto.thresholdType());
        entity.setEnabled(true);

        entity.setRoom(roomRepository.findById(dto.roomId())
                        .orElseThrow(() -> new RoomNotFoundException("Room with id " + dto.roomId() + " not found")));

        if (dto.climateHintIds() != null) {
            entity.setClimateHints(new HashSet<>(hintRepository.findAllById(dto.climateHintIds())));
        }
        return entity;
    }

    @Override
    public ThresholdCreateDTO mapTo(Threshold entity) {
        throw new UnsupportedOperationException();
    }
}

package at.qe.skeleton.absence.mapper;

import at.qe.skeleton.absence.dto.AbsenceDTO;
import at.qe.skeleton.common.DTOMapper;
import at.qe.skeleton.absence.model.Absence;
import org.springframework.stereotype.Service;

@Service
public class AbsenceMapper implements DTOMapper<Absence, AbsenceDTO> {

    @Override
    public AbsenceDTO mapTo(Absence entity) {
        return new AbsenceDTO(
                entity.getId(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getAbsenceType(),
                entity.getAbsenceStatus(),
                entity.getUser().getId()
        );
    }

    @Override
    public Absence mapFrom(AbsenceDTO dto) {
        throw new UnsupportedOperationException();
    }
}

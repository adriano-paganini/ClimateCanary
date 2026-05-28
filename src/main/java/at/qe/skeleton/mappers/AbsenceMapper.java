package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.AbsenceDTO;
import at.qe.skeleton.common.DTOMapper;
import at.qe.skeleton.models.Absence;
import at.qe.skeleton.models.Userx;
import org.springframework.stereotype.Service;

@Service
public class AbsenceMapper implements DTOMapper<Absence, AbsenceDTO> {

    @Override
    public AbsenceDTO mapTo(Absence entity) {
        Userx user = entity.getUser();
        return new AbsenceDTO(
                entity.getId(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getAbsenceType(),
                entity.getAbsenceStatus(),
                user != null ? user.getId() : null,
                user != null ? user.getUsername() : null,
                user != null ? user.getFirstName() : null,
                user != null ? user.getLastName() : null
        );
    }

    @Override
    public Absence mapFrom(AbsenceDTO dto) {
        throw new UnsupportedOperationException();
    }
}

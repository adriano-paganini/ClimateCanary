package at.qe.skeleton.absence.mapper;

import at.qe.skeleton.absence.dto.AbsenceCreateDTO;
import at.qe.skeleton.common.DTOMapper;
import at.qe.skeleton.absence.model.Absence;
import at.qe.skeleton.absence.model.AbsenceStatus;
import at.qe.skeleton.userx.service.UserxService;
import org.springframework.stereotype.Service;

@Service
public class AbsenceCreateMapper implements DTOMapper<Absence, AbsenceCreateDTO> {


    private final UserxService userxService;

    public AbsenceCreateMapper(UserxService userxService) {
        this.userxService = userxService;
    }

    @Override
    public AbsenceCreateDTO mapTo(Absence entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Absence mapFrom(AbsenceCreateDTO dto) {
        Absence absence = new Absence();
        absence.setUser(userxService.getUserById(dto.userxId()));
        absence.setStartDate(dto.startDate());
        absence.setEndDate(dto.endDate());
        absence.setAbsenceType(dto.absenceType());
        absence.setAbsenceStatus(AbsenceStatus.PLANNED);
        return absence;
    }
}

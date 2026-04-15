package at.qe.skeleton.absence.service;

import at.qe.skeleton.absence.dto.AbsenceUpdateDTO;
import at.qe.skeleton.common.exceptions.AbsenceNotFoundException;
import at.qe.skeleton.absence.model.Absence;
import at.qe.skeleton.absence.repository.AbsenceRepository;
import at.qe.skeleton.userx.service.UserxService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AbsenceService {

    private final AbsenceRepository absenceRepository;
    private final UserxService userxService;

    public AbsenceService(AbsenceRepository absenceRepository,
                          UserxService userxService) {
        this.absenceRepository = absenceRepository;
        this.userxService = userxService;
    }

    public List<Absence> getAll() {
        return absenceRepository.findAll();
    }

    public Absence getById(Long id) {
        return absenceRepository.findById(id)
                .orElseThrow(() -> new AbsenceNotFoundException("Absence with id " + id + " not found"));
    }

    public Absence create(Absence absence) {
        return absenceRepository.save(absence);
    }

    public Absence update(Long id, AbsenceUpdateDTO dto) {
        Absence existing = getById(id);

        if (dto.startDate() != null) {
            existing.setStartDate(dto.startDate());
        }

        if (dto.endDate() != null) {
            existing.setEndDate(dto.endDate());
        }

        if (dto.absenceType() != null) {
            existing.setAbsenceType(dto.absenceType());
        }

        if (dto.absenceStatus() != null) {
            existing.setAbsenceStatus(dto.absenceStatus());
        }

        if (dto.userxId() != null) {
            existing.setUser(userxService.getUserById(dto.userxId()));
        }

        return absenceRepository.save(existing);
    }

    public void delete(Long id) {
        getById(id);
        absenceRepository.deleteById(id);
    }
}
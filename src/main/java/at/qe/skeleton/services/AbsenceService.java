package at.qe.skeleton.services;

import at.qe.skeleton.dtos.AbsenceUpdateDTO;
import at.qe.skeleton.models.Absence;
import at.qe.skeleton.repositories.AbsenceRepository;
import at.qe.skeleton.common.exceptions.AbsenceNotFoundException;
import at.qe.skeleton.models.Userx;
import at.qe.skeleton.models.UserxRole;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
public class AbsenceService {

    private final AbsenceRepository absenceRepository;
    private final UserxService userxService;
    private final AuthenticatedUserService authenticatedUserService;

    public AbsenceService(AbsenceRepository absenceRepository,
                          UserxService userxService, AuthenticatedUserService authenticatedUserService) {
        this.absenceRepository = absenceRepository;
        this.userxService = userxService;
        this.authenticatedUserService = authenticatedUserService;
    }

    public List<Absence> getAll() {
        return absenceRepository.findAll();
    }

    public Absence getById(Long id) {
        return absenceRepository.findById(id)
                .orElseThrow(() -> new AbsenceNotFoundException("Absence with id " + id + " not found"));
    }

    public Collection<Absence> getAbsencesForUser(Userx user) {
        Userx authenticatedUser = authenticatedUserService.getAuthenticatedUser();
        boolean isSelf = authenticatedUser.getId().equals(user.getId());
        boolean isAdmin = authenticatedUser.getRoles().contains(UserxRole.SYSTEM_ADMIN);
        if (!isSelf && !isAdmin) {
            throw new AccessDeniedException("You may only view your own absences.");
        }
        return absenceRepository.findByUser(user);
    }

    public Absence create(Absence absence) {

        if (absence.getEndDate() != null && absence.getStartDate() != null
                && absence.getEndDate().isBefore(absence.getStartDate())) {
            throw new IllegalArgumentException("Absence end date must not be before start date");
        }
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
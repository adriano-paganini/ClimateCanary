package at.qe.skeleton.services;

import at.qe.skeleton.common.exceptions.NotFoundException;
import at.qe.skeleton.dtos.AbsenceUpdateDTO;
import at.qe.skeleton.models.Absence;
import at.qe.skeleton.repositories.AbsenceRepository;
import at.qe.skeleton.models.Userx;
import at.qe.skeleton.models.UserxRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Slf4j
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
                .orElseThrow(() -> new NotFoundException("Absence with id " + id + " not found"));
    }

    public Collection<Absence> getAbsencesForUser(Userx user) {
        if (user == null) {
            throw new NotFoundException("User cannot be null");
        }
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
        Absence savedAbsence =  absenceRepository.save(absence);
        log.info("Created absence with ID: {} for User with ID: {}",absence.getId(),absence.getUser().getId());

        log.debug("Created absence details: id={}, UserId={}, startDate={}, endDate={}, absenceType={}, absenceStatus={}",
                savedAbsence.getId(),
                savedAbsence.getUser().getId(),
                savedAbsence.getStartDate(),
                savedAbsence.getEndDate(),
                savedAbsence.getAbsenceType(),
                savedAbsence.getAbsenceStatus());

        return savedAbsence;
    }

    public Absence update(Long id, AbsenceUpdateDTO dto) {
        Absence existing = getById(id);
        Long actorId = authenticatedUserService.getAuthenticatedUser().getId();

        StringBuilder debugInfo = new StringBuilder("Updated absence details:")
                .append(" id=").append(id)
                .append(", actorId=").append(actorId);

        if (dto.userxId() != null) {
            existing.setUser(userxService.getUserById(dto.userxId()));
            debugInfo.append(", newUserId=").append(dto.userxId());
        }

        if (dto.startDate() != null) {
            existing.setStartDate(dto.startDate());
            debugInfo.append(", startDate=").append(dto.startDate());
        }

        if (dto.endDate() != null) {
            existing.setEndDate(dto.endDate());
            debugInfo.append(", endDate=").append(dto.endDate());
        }

        if (dto.absenceType() != null) {
            existing.setAbsenceType(dto.absenceType());
            debugInfo.append(", absenceType=").append(dto.absenceType());
        }

        if (dto.absenceStatus() != null) {
            existing.setAbsenceStatus(dto.absenceStatus());
            debugInfo.append(", absenceStatus=").append(dto.absenceStatus());
        }

        Absence updatedAbsence = absenceRepository.save(existing);

        log.info("Updated absence id={} by userId={}", id, actorId);
        log.debug(debugInfo.toString());

        return updatedAbsence;
    }

    public void delete(Long id) {
        getById(id);
        absenceRepository.deleteById(id);
        log.info("Deleted absence with id: {}", id);
    }
}
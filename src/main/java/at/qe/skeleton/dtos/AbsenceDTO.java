package at.qe.skeleton.dtos;

import at.qe.skeleton.models.AbsenceStatus;
import at.qe.skeleton.models.AbsenceType;

import java.time.LocalDateTime;

public record AbsenceDTO(
        Long id,
        LocalDateTime startDate,
        LocalDateTime endDate,
        AbsenceType absenceType,
        AbsenceStatus absenceStatus,
        Long userxId,
        String username,
        String userFirstName,
        String userLastName
) {}

package at.qe.skeleton.absence.dto;

import at.qe.skeleton.absence.model.AbsenceStatus;
import at.qe.skeleton.absence.model.AbsenceType;

import java.time.LocalDateTime;

public record AbsenceDTO(
        Long id,
        LocalDateTime startDate,
        LocalDateTime endDate,
        AbsenceType absenceType,
        AbsenceStatus absenceStatus,
        Long userxId
) {}

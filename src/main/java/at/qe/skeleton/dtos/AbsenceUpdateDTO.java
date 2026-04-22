package at.qe.skeleton.dtos;

import at.qe.skeleton.models.AbsenceStatus;
import at.qe.skeleton.models.AbsenceType;

import java.time.LocalDateTime;

public record AbsenceUpdateDTO(
        LocalDateTime startDate,
        LocalDateTime endDate,
        AbsenceType absenceType,
        Long userxId,
        AbsenceStatus absenceStatus
) {}

package at.qe.skeleton.absence.dto;

import at.qe.skeleton.absence.model.AbsenceType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record AbsenceCreateDTO(

        @NotNull
        LocalDateTime startDate,

        @NotNull
        LocalDateTime endDate,

        @NotNull
        AbsenceType absenceType,

        @NotNull
        Long userxId
) {}

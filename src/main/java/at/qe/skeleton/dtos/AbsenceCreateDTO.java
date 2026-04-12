package at.qe.skeleton.dtos;

import at.qe.skeleton.model.AbsenceType;
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

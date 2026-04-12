package at.qe.skeleton.dtos;

import jakarta.validation.constraints.NotNull;

public record EmployeeProfileCreateDTO(

        @NotNull
        Long userxId,

        @NotNull
        Long departmentId,

        @NotNull
        Long roomId
) {}

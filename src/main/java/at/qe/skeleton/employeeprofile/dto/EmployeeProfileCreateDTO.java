package at.qe.skeleton.employeeprofile.dto;

import jakarta.validation.constraints.NotNull;

public record EmployeeProfileCreateDTO(

        @NotNull
        Long userxId,

        @NotNull
        Long departmentId,

        @NotNull
        Long roomId
) {}

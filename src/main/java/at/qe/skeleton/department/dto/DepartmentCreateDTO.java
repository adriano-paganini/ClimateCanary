package at.qe.skeleton.department.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record DepartmentCreateDTO(

        @NotBlank
        String name,

        List<Long> roomIds,

        @NotNull
        Long departmentLeadId
) {}

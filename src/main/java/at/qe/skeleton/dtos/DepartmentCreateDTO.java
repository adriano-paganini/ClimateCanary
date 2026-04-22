package at.qe.skeleton.dtos;

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

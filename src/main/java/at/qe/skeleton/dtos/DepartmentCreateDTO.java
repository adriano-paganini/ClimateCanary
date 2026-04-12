package at.qe.skeleton.dtos;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record DepartmentCreateDTO(

        @NotNull
        String name,
        List<Long> roomIds,

        @NotNull
        Long departmentLeadId
) {
}

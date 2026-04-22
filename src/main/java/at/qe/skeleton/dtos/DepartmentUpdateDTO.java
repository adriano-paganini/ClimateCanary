package at.qe.skeleton.dtos;

import jakarta.validation.constraints.Size;

import java.util.List;

public record DepartmentUpdateDTO(

        @Size(min = 1, message = "Name must not be blank if provided")
        String name,

        List<Long> roomIds,

        Long departmentLeadId
) {}

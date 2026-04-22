package at.qe.skeleton.dtos;

import java.util.List;

public record DepartmentDTO(
        Long id,
        String name,
        List<Long> roomIds,
        Long departmentLeadId
) {}

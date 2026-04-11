package at.qe.skeleton.dtos;

import java.util.List;

public record DepartmentCreateDTO(
        String name,
        List<Long> roomIds,
        Long departmentLeadId
) {
}

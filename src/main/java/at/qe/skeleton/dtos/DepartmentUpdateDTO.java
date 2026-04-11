package at.qe.skeleton.dtos;

import java.util.List;

public record DepartmentUpdateDTO(
        String name,
        List<Long> roomIds,
        Long departmentLeadId
) {
}

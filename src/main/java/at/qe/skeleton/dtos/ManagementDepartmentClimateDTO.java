package at.qe.skeleton.dtos;

import java.util.List;

public record ManagementDepartmentClimateDTO(
        Long departmentId,
        String departmentName,
        int activeWarnings,
        List<ViolationBreakdownDTO> warningsByMetric,
        List<ManagementClimateTrendDTO> trends
) {}

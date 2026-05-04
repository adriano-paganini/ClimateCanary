package at.qe.skeleton.dtos;

import at.qe.skeleton.models.Metric;

import java.util.Map;

public record DepartmentDashboardDTO(
        Long departmentId,
        String departmentName,
        int activeViolations,
        Map<Metric, Long> avgMetrics
) {}

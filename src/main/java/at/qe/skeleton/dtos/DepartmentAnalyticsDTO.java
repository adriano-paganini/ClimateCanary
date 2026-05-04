package at.qe.skeleton.dtos;

import at.qe.skeleton.models.Metric;

import java.util.Map;

public record DepartmentAnalyticsDTO(
        Long departmentId,
        String departmentName,
        int roomCount,
        int activeViolations,
        Map<Metric, Long> avgMetrics
) {}

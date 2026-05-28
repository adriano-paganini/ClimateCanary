package at.qe.skeleton.dtos;

import at.qe.skeleton.models.Metric;

public record ManagementClimateTrendDTO(
        Metric metric,
        String weeklyDirection,
        String monthlyDirection
) {}

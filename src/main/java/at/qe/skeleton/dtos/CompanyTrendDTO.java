package at.qe.skeleton.dtos;

import at.qe.skeleton.models.Metric;

import java.util.List;

public record CompanyTrendDTO(
        Metric metric,
        String bucketSize,
        List<TrendPointDTO> points
) {}

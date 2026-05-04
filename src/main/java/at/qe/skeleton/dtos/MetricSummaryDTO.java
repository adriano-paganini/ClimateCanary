package at.qe.skeleton.dtos;

public record MetricSummaryDTO(
        Long latest,
        Long avg,
        Long min,
        Long max,
        Long count
) {}

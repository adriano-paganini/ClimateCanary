package at.qe.skeleton.dtos;

public record MetricSummaryDTO(
        Double latest,
        Double avg,
        Double min,
        Double max,
        Long count
) {}

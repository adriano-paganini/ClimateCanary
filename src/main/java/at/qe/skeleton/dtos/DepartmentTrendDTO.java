package at.qe.skeleton.dtos;

import at.qe.skeleton.models.Metric;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record DepartmentTrendDTO(
        Metric metric,

        @Schema(description = "Resolved aggregation bucket size", allowableValues = {"raw", "1h", "6h", "1d", "1w"})
        String bucketSize,

        List<TrendPointDTO> points
) {}

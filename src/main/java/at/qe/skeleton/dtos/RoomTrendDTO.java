package at.qe.skeleton.dtos;

import at.qe.skeleton.models.Metric;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record RoomTrendDTO(
        Long roomId,
        Metric metric,

        @Schema(description = "Resolved aggregation bucket size", allowableValues = {"raw", "1h", "6h", "1d", "1w"})
        String bucketSize,

        @Schema(description = "True when privacy rules forced coarser aggregation")
        boolean granularityReduced,

        List<TrendPointDTO> points
) {}

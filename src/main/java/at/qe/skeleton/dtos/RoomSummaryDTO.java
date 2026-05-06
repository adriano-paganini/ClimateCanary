package at.qe.skeleton.dtos;

import at.qe.skeleton.models.Metric;

import java.time.LocalDateTime;
import java.util.Map;

public record RoomSummaryDTO(
        Long roomId,
        String roomName,
        LocalDateTime generatedAt,
        Map<Metric, MetricSummaryDTO> metrics
) {}

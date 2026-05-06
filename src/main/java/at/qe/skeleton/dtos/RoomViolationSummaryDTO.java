package at.qe.skeleton.dtos;

import java.util.List;

public record RoomViolationSummaryDTO(
        Long roomId,
        String roomName,
        int total,
        int active,
        int resolved,
        List<ViolationBreakdownDTO> byMetric
) {}

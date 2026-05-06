package at.qe.skeleton.dtos;

import java.util.List;

public record ViolationSummaryDTO(
        int total,
        int active,
        int resolved,
        List<ViolationBreakdownDTO> byMetric,
        List<ViolationBreakdownDTO> byRoom
) {}

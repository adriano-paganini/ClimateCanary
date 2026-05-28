package at.qe.skeleton.dtos;

import java.time.LocalDateTime;
import java.util.List;

public record ManagementClimateDashboardDTO(
        LocalDateTime generatedAt,
        int totalActiveWarnings,
        List<ManagementDepartmentClimateDTO> departments
) {}

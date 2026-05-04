package at.qe.skeleton.dtos;

import java.time.LocalDateTime;
import java.util.List;

public record CompanyDashboardDTO(
        LocalDateTime generatedAt,
        int totalRooms,
        int totalEmployees,
        int activeViolations,
        List<DepartmentDashboardDTO> departments
) {}

package at.qe.skeleton.controllers;

import at.qe.skeleton.dtos.*;
import at.qe.skeleton.models.Metric;
import at.qe.skeleton.services.AnalyticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/rooms/{id}/summary")
    public ResponseEntity<RoomSummaryDTO> getRoomSummary(@PathVariable Long id) {
        return ResponseEntity.ok(analyticsService.getRoomSummary(id));
    }

    @GetMapping("/rooms/{id}/trends")
    public ResponseEntity<RoomTrendDTO> getRoomTrend(
            @PathVariable Long id,
            @RequestParam(required = false) Metric metric,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        Metric effectiveMetric = metric != null ? metric : Metric.TEMPERATURE;
        return ResponseEntity.ok(analyticsService.getRoomTrend(id, effectiveMetric, from, to));
    }

    @GetMapping("/departments/{id}/summary")
    public ResponseEntity<DepartmentAnalyticsDTO> getDepartmentSummary(@PathVariable Long id) {
        return ResponseEntity.ok(analyticsService.getDepartmentSummary(id));
    }

    @GetMapping("/departments/{id}/violations")
    public ResponseEntity<ViolationSummaryDTO> getDepartmentViolations(@PathVariable Long id) {
        return ResponseEntity.ok(analyticsService.getDepartmentViolationSummary(id));
    }

    @GetMapping("/company/dashboard")
    public ResponseEntity<CompanyDashboardDTO> getCompanyDashboard() {
        return ResponseEntity.ok(analyticsService.getCompanyDashboard());
    }

    @GetMapping("/company/violations")
    public ResponseEntity<ViolationSummaryDTO> getCompanyViolations() {
        return ResponseEntity.ok(analyticsService.getCompanyViolationSummary());
    }

    @GetMapping("/company/trends")
    public ResponseEntity<CompanyTrendDTO> getCompanyTrend(
            @RequestParam(required = false) Metric metric,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        Metric effectiveMetric = metric != null ? metric : Metric.TEMPERATURE;
        return ResponseEntity.ok(analyticsService.getCompanyTrend(effectiveMetric, from, to));
    }
}

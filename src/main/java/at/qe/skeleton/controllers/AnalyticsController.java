package at.qe.skeleton.controllers;

import at.qe.skeleton.dtos.*;
import at.qe.skeleton.models.Metric;
import at.qe.skeleton.services.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/*
DISCLAIMER:

AI generated Swagger Method comments with minimal changes to save time
 */

/**
 * REST controller exposing analytics and data-aggregation endpoints.
 *
 * <p>All endpoints require an authenticated session. Role enforcement is applied both at
 * the controller level (via {@code @PreAuthorize}) and inside
 * {@link AnalyticsService} for fine-grained ownership checks.</p>
 *
 * <p><b>Base path:</b> {@code /api/analytics}</p>
 */
@Tag(
        name = "Analytics",
        description = """
        Climate data aggregation endpoints.
        Access is role-scoped:
        EMPLOYEE → own room + department common areas;
        DEPARTMENT_LEAD → full department (reduced granularity for offices);
        MANAGEMENT → anonymized department/company aggregates;
        BUILDING_ADMIN → all rooms at full granularity.
        SYSTEM_ADMIN has no access to any analytics endpoint.
        """
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * Retrieves a statistical summary (latest, avg, min, max, count) for every metric
     * in the specified room over the given time window.
     *
     * <p><b>Access:</b> EMPLOYEE (own room / department common areas, occupancy guard applies),
     * DEPARTMENT_LEAD (any room in their department), BUILDING_ADMIN (any room).</p>
     */
    @Operation(
            summary = "Room climate summary",
            description = """
            Returns the latest value, average, minimum, maximum, and measurement count for
            every climate metric recorded in the room during the requested window.

            **Privacy / occupancy guard:** An EMPLOYEE can only retrieve data for their own
            office when the room's minimum occupancy requirement is met (privacy mode OFF).
            Common areas of the employee's department are always accessible.

            **Default window:** last 24 hours when `from`/`to` are omitted.
            """,
            responses = {
            @ApiResponse(responseCode = "200", description = "Summary returned successfully",
                    content = @Content(schema = @Schema(implementation = RoomSummaryDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid date range (from > to)",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied – insufficient role or occupancy not met",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Room not found",
                    content = @Content)
    })
    @PreAuthorize("hasAnyAuthority('EMPLOYEE','DEPARTMENT_LEAD','BUILDING_ADMIN')")
    @GetMapping("/rooms/{roomId}/summary")
    public ResponseEntity<RoomSummaryDTO> getRoomSummary(
            @Parameter(description = "ID of the room", required = true)
            @PathVariable Long roomId,
            @Parameter(description = "Window start (ISO-8601). Defaults to `to` minus 24 h.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "Window end (ISO-8601). Defaults to now.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(analyticsService.getRoomSummary(roomId, from, to));
    }

    @Operation(
            summary = "Room violation summary",
            description = """
            Returns total, active, and resolved violation counts for a single room,
            broken down by metric.
    
            **Access:** EMPLOYEE (own room / department common areas, occupancy guard applies),
            DEPARTMENT_LEAD (any room in their department), BUILDING_ADMIN (any room).
            """,
            responses = {
            @ApiResponse(responseCode = "200", description = "Violation summary returned successfully",
                    content = @Content(schema = @Schema(implementation = RoomViolationSummaryDTO.class))),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Room not found",
                    content = @Content)
    })
    @PreAuthorize("hasAnyAuthority('EMPLOYEE','DEPARTMENT_LEAD','BUILDING_ADMIN')")
    @GetMapping("/rooms/{roomId}/violations")
    public ResponseEntity<RoomViolationSummaryDTO> getRoomViolations(
            @Parameter(description = "ID of the room", required = true)
            @PathVariable Long roomId) {
        return ResponseEntity.ok(analyticsService.getRoomViolationSummary(roomId));
    }

    /**
     * Retrieves a time-series trend for a single climate metric in the specified room.
     *
     * <p><b>Granularity rules:</b></p>
     * <ul>
     *   <li>EMPLOYEE / BUILDING_ADMIN – automatic bucketing (raw ≤ 7 d, 1 h ≤ 90 d,
     *       6 h ≤ 1 y, 1 d beyond).</li>
     *   <li>DEPARTMENT_LEAD viewing an <em>occupied</em> office (privacy mode OFF) –
     *       forced reduced granularity (daily averages for ≤ 30 d, weekly beyond).</li>
     *   <li>DEPARTMENT_LEAD viewing a common area – full automatic granularity.</li>
     * </ul>
     */
    @Operation(
            summary = "Room climate trend",
            description = """
            Returns a time-series of average metric values bucketed by an automatically
            resolved (or forced-reduced) bucket size.

            **Bucket sizes:** `raw` | `1h` | `6h` | `1d` | `1w`

            **Granularity for DEPARTMENT_LEAD + office rooms:** Always at least daily averages
            (`1d`) to protect individual privacy, per §1.5.3 of the specification. Common areas
            are returned at full granularity.

            **Default window:** last 24 hours when `from`/`to` are omitted.
            Metric defaults to `TEMPERATURE` when omitted.
            """,
            responses = {
            @ApiResponse(responseCode = "200", description = "Trend returned successfully",
                    content = @Content(schema = @Schema(implementation = RoomTrendDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid date range",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Room not found",
                    content = @Content)
    })
    @PreAuthorize("hasAnyAuthority('EMPLOYEE','DEPARTMENT_LEAD','BUILDING_ADMIN')")
    @GetMapping("/rooms/{roomId}/trends")
    public ResponseEntity<RoomTrendDTO> getRoomTrend(
            @Parameter(description = "ID of the room", required = true)
            @PathVariable Long roomId,
            @Parameter(description = "Climate metric to retrieve. Defaults to `TEMPERATURE`.")
            @RequestParam(required = false) Metric metric,
            @Parameter(description = "Window start (ISO-8601). Defaults to `to` minus 24 h.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "Window end (ISO-8601). Defaults to now.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        Metric effectiveMetric = metric != null ? metric : Metric.TEMPERATURE;
        return ResponseEntity.ok(analyticsService.getRoomTrend(roomId, effectiveMetric, from, to));
    }

    /**
     * Retrieves an aggregated summary for an entire department.
     *
     * <p>Returns the number of active rooms, total active threshold violations, and average
     * metric values across all active rooms in the department for the given window.</p>
     *
     * <p><b>Access:</b> DEPARTMENT_LEAD (own department only), MANAGEMENT, BUILDING_ADMIN.</p>
     */
    @Operation(
            summary = "Department climate summary",
            description = """
            Returns aggregate statistics for all active rooms in the department:
            room count, active violation count, and average value per metric.

            **DEPARTMENT_LEAD** may only query their own department.

            **Default window:** last 24 hours when `from`/`to` are omitted.
            """,
            responses = {
            @ApiResponse(responseCode = "200", description = "Summary returned successfully",
                    content = @Content(schema = @Schema(implementation = DepartmentAnalyticsDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid date range",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Department not found",
                    content = @Content)
    })
    @PreAuthorize("hasAnyAuthority('DEPARTMENT_LEAD','MANAGEMENT','BUILDING_ADMIN')")
    @GetMapping("/departments/{departmentId}/summary")
    public ResponseEntity<DepartmentAnalyticsDTO> getDepartmentSummary(
            @Parameter(description = "ID of the department", required = true)
            @PathVariable Long departmentId,
            @Parameter(description = "Window start (ISO-8601). Defaults to `to` minus 24 h.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "Window end (ISO-8601). Defaults to now.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(analyticsService.getDepartmentSummary(departmentId, from, to));
    }

    /**
     * Retrieves threshold violation counts for a department, broken down by metric and room.
     *
     * <p><b>Anonymization:</b> MANAGEMENT receives a metric-level breakdown only; the room-level
     * breakdown is omitted so violations cannot be traced to individual rooms.</p>
     *
     * <p><b>Access:</b> DEPARTMENT_LEAD (own department only), MANAGEMENT, BUILDING_ADMIN.</p>
     */
    @Operation(
            summary = "Department violation summary",
            description = """
            Returns total, active, and resolved violation counts for the department,
            broken down by metric.

            **MANAGEMENT** receives an anonymized response: the `byRoom` breakdown is omitted
            so that violations cannot be traced to individual rooms.

            **DEPARTMENT_LEAD** may only query their own department.
            **BUILDING_ADMIN** receives the full room-level breakdown.
            """,
            responses = {
            @ApiResponse(responseCode = "200", description = "Violation summary returned successfully",
                    content = @Content(schema = @Schema(implementation = ViolationSummaryDTO.class))),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Department not found",
                    content = @Content)
    })
    @PreAuthorize("hasAnyAuthority('DEPARTMENT_LEAD','MANAGEMENT','BUILDING_ADMIN')")
    @GetMapping("/departments/{departmentId}/violations")
    public ResponseEntity<ViolationSummaryDTO> getDepartmentViolations(
            @Parameter(description = "ID of the department", required = true)
            @PathVariable Long departmentId) {
        return ResponseEntity.ok(analyticsService.getDepartmentViolationSummary(departmentId));
    }

    /**
     * Retrieves an aggregated time-series trend for a single metric across all active rooms
     * in a department.
     *
     * <p>Intended for week/month trend-indicator views (improvement or deterioration of
     * climate conditions over time).</p>
     *
     * <p><b>Access:</b> DEPARTMENT_LEAD (own department only), MANAGEMENT, BUILDING_ADMIN.</p>
     */
    @Operation(
            summary = "Department climate trend",
            description = """
            Returns a time-series of average metric values aggregated across all active
            rooms in the department, bucketed by an automatically resolved size.

            **Bucket sizes:** `raw` | `1h` | `6h` | `1d` | `1w`

            Use this endpoint for trend-indicator views (improvement / deterioration over
            weeks and months at department level).

            **DEPARTMENT_LEAD** may only query their own department.

            **Default window:** last 7 days when `from`/`to` are omitted.
            Metric defaults to `TEMPERATURE` when omitted.
            """,
            responses = {
            @ApiResponse(responseCode = "200", description = "Trend returned successfully",
                    content = @Content(schema = @Schema(implementation = DepartmentTrendDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid date range",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Department not found",
                    content = @Content)
    })
    @PreAuthorize("hasAnyAuthority('DEPARTMENT_LEAD','MANAGEMENT','BUILDING_ADMIN')")
    @GetMapping("/departments/{departmentId}/trends")
    public ResponseEntity<DepartmentTrendDTO> getDepartmentTrend(
            @Parameter(description = "ID of the department", required = true)
            @PathVariable Long departmentId,
            @Parameter(description = "Climate metric to aggregate. Defaults to `TEMPERATURE`.")
            @RequestParam(required = false) Metric metric,
            @Parameter(description = "Window start (ISO-8601). Defaults to `to` minus 7 d.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "Window end (ISO-8601). Defaults to now.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        Metric effectiveMetric = metric != null ? metric : Metric.TEMPERATURE;
        return ResponseEntity.ok(analyticsService.getDepartmentTrend(
                departmentId, effectiveMetric, from, to));
    }

    /**
     * Retrieves a company-wide dashboard aggregating employee count, total active rooms,
     * total active violations, and per-department metric averages.
     *
     * <p>This is the primary entry point for MANAGEMENT's company-level monitoring and
     * for BUILDING_ADMIN operational oversight.</p>
     */
    @Operation(
            summary = "Company-wide dashboard",
            description = """
            Returns a snapshot of the entire company's climate status:
            - Total employees and active rooms
            - Total currently active threshold violations
            - Per-department breakdown: active violations and average metric values

            This is the top-level dashboard for MANAGEMENT trend monitoring and
            BUILDING_ADMIN operational oversight.

            **Default window:** last 7 days when `from`/`to` are omitted.
            """,
            responses = {
            @ApiResponse(responseCode = "200", description = "Dashboard returned successfully",
                    content = @Content(schema = @Schema(implementation = CompanyDashboardDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid date range",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content)
    })
    @PreAuthorize("hasAnyAuthority('MANAGEMENT','BUILDING_ADMIN')")
    @GetMapping("/company/dashboard")
    public ResponseEntity<CompanyDashboardDTO> getCompanyDashboard(
            @Parameter(description = "Window start (ISO-8601). Defaults to `to` minus 7 d.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "Window end (ISO-8601). Defaults to now.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(analyticsService.getCompanyDashboard(from, to));
    }

    @Operation(
            summary = "Management climate dashboard",
            description = """
            Returns only department-level climate changes and active warning counts for
            management users. The response does not include room-level data, raw values,
            averages, or time-series measurements.
            """,
            responses = {
            @ApiResponse(responseCode = "200", description = "Dashboard returned successfully",
                    content = @Content(schema = @Schema(implementation = ManagementClimateDashboardDTO.class))),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content)
    })
    @PreAuthorize("hasAuthority('MANAGEMENT')")
    @GetMapping("/company/management-climate")
    public ResponseEntity<ManagementClimateDashboardDTO> getManagementClimateDashboard() {
        return ResponseEntity.ok(analyticsService.getManagementClimateDashboard());
    }

    /**
     * Retrieves a company-wide violation summary broken down by metric and (conditionally) by room.
     *
     * <p><b>Anonymization:</b> MANAGEMENT receives a metric-level breakdown only; the room-level
     * list is omitted so that violations remain non-traceable to individual rooms.</p>
     */
    @Operation(
            summary = "Company-wide violation summary",
            description = """
            Returns total, active, and resolved violation counts across the entire company,
            broken down by metric.

            **MANAGEMENT** receives an anonymized response: `byRoom` is always empty,
            ensuring violations cannot be traced to individual rooms.

            **BUILDING_ADMIN** receives the full room-level breakdown.
            """,
            responses = {
            @ApiResponse(responseCode = "200", description = "Violation summary returned successfully",
                    content = @Content(schema = @Schema(implementation = ViolationSummaryDTO.class))),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content)
    })
    @PreAuthorize("hasAnyAuthority('MANAGEMENT','BUILDING_ADMIN')")
    @GetMapping("/company/violations")
    public ResponseEntity<ViolationSummaryDTO> getCompanyViolations() {
        return ResponseEntity.ok(analyticsService.getCompanyViolationSummary());
    }

    /**
     * Retrieves a company-wide time-series trend for a single metric, aggregated across
     * all active rooms in all departments.
     *
     * <p>Designed for MANAGEMENT trend-indicator views (climate improvement or deterioration
     * over weeks/months at company level) and BUILDING_ADMIN operational analysis.</p>
     */
    @Operation(
            summary = "Company-wide climate trend",
            description = """
            Returns a time-series of average metric values aggregated across every active
            room in the company, bucketed by an automatically resolved size.

            **Bucket sizes:** `raw` | `1h` | `6h` | `1d` | `1w`

            Use this endpoint for trend-indicator views showing improvement or deterioration
            of climate conditions over weeks and months at company level.

            **Default window:** last 7 days when `from`/`to` are omitted.
            Metric defaults to `TEMPERATURE` when omitted.
            """,
            responses = {
            @ApiResponse(responseCode = "200", description = "Trend returned successfully",
                    content = @Content(schema = @Schema(implementation = CompanyTrendDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid date range",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content)
    })
    @PreAuthorize("hasAnyAuthority('MANAGEMENT','BUILDING_ADMIN')")
    @GetMapping("/company/trends")
    public ResponseEntity<CompanyTrendDTO> getCompanyTrend(
            @Parameter(description = "Climate metric to aggregate. Defaults to `TEMPERATURE`.")
            @RequestParam(required = false) Metric metric,
            @Parameter(description = "Window start (ISO-8601). Defaults to `to` minus 7 d.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "Window end (ISO-8601). Defaults to now.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        Metric effectiveMetric = metric != null ? metric : Metric.TEMPERATURE;
        return ResponseEntity.ok(analyticsService.getCompanyTrend(effectiveMetric, from, to));
    }
}

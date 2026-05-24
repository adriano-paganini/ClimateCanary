package at.qe.skeleton.tests.services;

import at.qe.skeleton.dtos.RoomSummaryDTO;
import at.qe.skeleton.dtos.RoomTrendDTO;
import at.qe.skeleton.dtos.RoomViolationSummaryDTO;
import at.qe.skeleton.models.Metric;
import at.qe.skeleton.models.Threshold;
import at.qe.skeleton.models.ThresholdViolation;
import at.qe.skeleton.models.ViolationStatus;
import at.qe.skeleton.services.AnalyticsService;
import at.qe.skeleton.services.BuildingService;
import at.qe.skeleton.services.DepartmentService;
import at.qe.skeleton.services.RoomService;
import at.qe.skeleton.services.ThresholdService;
import at.qe.skeleton.services.ThresholdViolationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.web.WebAppConfiguration;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@WebAppConfiguration
@WithMockUser(username = "admin", authorities = {"BUILDING_ADMIN"})
@Tag("performance")
class RoomAdminRoomViewQueryPerformanceTest {

    private static final LocalDateTime FROM = LocalDateTime.of(2026, 4, 24, 0, 0);
    private static final LocalDateTime TO = LocalDateTime.of(2026, 5, 24, 0, 0);
    private static final Duration SMALL_QUERY_BUDGET = Duration.ofSeconds(2);
    private static final Duration ANALYTICS_QUERY_BUDGET = Duration.ofSeconds(5);
    private static final Duration FULL_ROOM_VIEW_BUDGET = Duration.ofSeconds(12);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RoomService roomService;

    @Autowired
    private BuildingService buildingService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private ThresholdViolationService thresholdViolationService;

    @Autowired
    private ThresholdService thresholdService;

    @Test
    @DisplayName("room admin reference data queries stay below timing budget")
    void roomAdminReferenceDataQueries_stayBelowTimingBudget() {
        assertThat(timed("GET /api/building", SMALL_QUERY_BUDGET, buildingService::getAllBuildings))
                .isNotEmpty();
        assertThat(timed("GET /api/department", SMALL_QUERY_BUDGET, departmentService::getAll))
                .isNotEmpty();
        assertThat(timed("GET /api/room", SMALL_QUERY_BUDGET, roomService::getAll))
                .isNotEmpty();
    }

    @Test
    @DisplayName("room admin selected room-view query bundle stays below timing budget")
    void roomAdminSelectedRoomQueryBundle_staysBelowTimingBudget() {
        Long roomId = roomIdByName("Room 1");
        Integer generatedMeasurements = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM MEASUREMENT
                WHERE ROOM_ID = ?
                  AND TIMESTAMP >= ?
                  AND TIMESTAMP < ?
                """,
                Integer.class,
                roomId,
                FROM,
                TO
        );
        assertThat(generatedMeasurements).isNotNull().isGreaterThanOrEqualTo(34_560);

        long start = System.nanoTime();

        RoomSummaryDTO summary = timed(
                "GET /api/analytics/rooms/{roomId}/summary",
                ANALYTICS_QUERY_BUDGET,
                () -> analyticsService.getRoomSummary(roomId, FROM, TO)
        );
        RoomViolationSummaryDTO violationSummary = timed(
                "GET /api/analytics/rooms/{roomId}/violations",
                SMALL_QUERY_BUDGET,
                () -> analyticsService.getRoomViolationSummary(roomId)
        );
        List<ThresholdViolation> activeViolations = timed(
                "GET /api/thresholdviolation?roomId=&violationStatus=ACTIVE",
                SMALL_QUERY_BUDGET,
                () -> thresholdViolationService.findAll(ViolationStatus.ACTIVE, roomId, null)
        );
        List<ThresholdViolation> resolvedViolations = timed(
                "GET /api/thresholdviolation?roomId=&violationStatus=RESOLVED",
                SMALL_QUERY_BUDGET,
                () -> thresholdViolationService.findAll(ViolationStatus.RESOLVED, roomId, null)
        );
        List<Threshold> thresholds = timed(
                "GET /api/threshold?roomId=",
                SMALL_QUERY_BUDGET,
                () -> thresholdService.getAll(roomId, null)
        );
        RoomTrendDTO trend = timed(
                "GET /api/analytics/rooms/{roomId}/trends",
                ANALYTICS_QUERY_BUDGET,
                () -> analyticsService.getRoomTrend(roomId, Metric.TEMPERATURE, FROM, TO)
        );

        long elapsedMillis = Duration.ofNanos(System.nanoTime() - start).toMillis();
        System.out.printf("Room admin selected room-view query bundle took %d ms%n", elapsedMillis);
        assertThat(elapsedMillis).isLessThan(FULL_ROOM_VIEW_BUDGET.toMillis());

        assertThat(summary.metrics().get(Metric.TEMPERATURE).count()).isGreaterThanOrEqualTo(8_640);
        assertThat(violationSummary.total()).isGreaterThanOrEqualTo(2);
        assertThat(activeViolations).isNotEmpty();
        assertThat(resolvedViolations).isNotEmpty();
        assertThat(thresholds).isNotEmpty();
        assertThat(trend.points()).isNotEmpty();
    }

    private Long roomIdByName(String roomName) {
        return jdbcTemplate.queryForObject(
                "SELECT ID FROM ROOMS WHERE NAME = ?",
                Long.class,
                roomName
        );
    }

    private <T> T timed(String label, Duration budget, Supplier<T> query) {
        long start = System.nanoTime();
        T result = query.get();
        long elapsedMillis = Duration.ofNanos(System.nanoTime() - start).toMillis();
        System.out.printf("Room admin room-view query '%s' took %d ms%n", label, elapsedMillis);
        assertThat(elapsedMillis).isLessThan(budget.toMillis());
        return result;
    }
}

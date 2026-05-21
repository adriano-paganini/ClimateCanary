package at.qe.skeleton.tests.controllers;

import at.qe.skeleton.dtos.CompanyDashboardDTO;
import at.qe.skeleton.dtos.CompanyTrendDTO;
import at.qe.skeleton.dtos.DepartmentAnalyticsDTO;
import at.qe.skeleton.dtos.DepartmentTrendDTO;
import at.qe.skeleton.dtos.RoomSummaryDTO;
import at.qe.skeleton.dtos.RoomTrendDTO;
import at.qe.skeleton.dtos.RoomViolationSummaryDTO;
import at.qe.skeleton.dtos.TrendPointDTO;
import at.qe.skeleton.dtos.ViolationBreakdownDTO;
import at.qe.skeleton.dtos.ViolationSummaryDTO;
import at.qe.skeleton.models.Metric;
import at.qe.skeleton.services.AnalyticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@WithMockUser(authorities = "BUILDING_ADMIN")
class AnalyticsControllerIntegrationTest {

    private static final LocalDateTime FROM = LocalDateTime.of(2026, 5, 18, 10, 0);
    private static final LocalDateTime TO = LocalDateTime.of(2026, 5, 18, 12, 0);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnalyticsService analyticsService;

    @Test
    void getRoomSummary_returns200AndDelegatesWithDateRange() throws Exception {
        RoomSummaryDTO dto = new RoomSummaryDTO(
                10L,
                "Office",
                TO,
                Map.of()
        );

        when(analyticsService.getRoomSummary(10L, FROM, TO)).thenReturn(dto);

        mockMvc.perform(get("/api/analytics/rooms/10/summary")
                        .param("from", "2026-05-18T10:00:00")
                        .param("to", "2026-05-18T12:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId", is(10)))
                .andExpect(jsonPath("$.roomName", is("Office")));

        verify(analyticsService).getRoomSummary(10L, FROM, TO);
    }

    @Test
    void getRoomViolations_returns200AndDelegates() throws Exception {
        RoomViolationSummaryDTO dto = new RoomViolationSummaryDTO(
                10L,
                "Office",
                3,
                1,
                2,
                List.of(new ViolationBreakdownDTO("TEMPERATURE", 3))
        );

        when(analyticsService.getRoomViolationSummary(10L)).thenReturn(dto);

        mockMvc.perform(get("/api/analytics/rooms/10/violations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId", is(10)))
                .andExpect(jsonPath("$.total", is(3)));

        verify(analyticsService).getRoomViolationSummary(10L);
    }

    @Test
    void getRoomTrend_defaultsMetricToTemperature() throws Exception {
        RoomTrendDTO dto = new RoomTrendDTO(
                10L,
                Metric.TEMPERATURE,
                "raw",
                false,
                List.of(new TrendPointDTO(FROM, 22.0))
        );

        when(analyticsService.getRoomTrend(10L, Metric.TEMPERATURE, null, null)).thenReturn(dto);

        mockMvc.perform(get("/api/analytics/rooms/10/trends"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId", is(10)))
                .andExpect(jsonPath("$.metric", is("TEMPERATURE")));

        verify(analyticsService).getRoomTrend(10L, Metric.TEMPERATURE, null, null);
    }

    @Test
    void getDepartmentSummary_returns200AndDelegatesWithDateRange() throws Exception {
        DepartmentAnalyticsDTO dto = new DepartmentAnalyticsDTO(
                3L,
                "Engineering",
                4,
                2,
                Map.of(Metric.TEMPERATURE, 22.5),
                Map.of(Metric.HUMIDITY, 45.0)
        );

        when(analyticsService.getDepartmentSummary(3L, FROM, TO)).thenReturn(dto);

        mockMvc.perform(get("/api/analytics/departments/3/summary")
                        .param("from", "2026-05-18T10:00:00")
                        .param("to", "2026-05-18T12:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departmentId", is(3)))
                .andExpect(jsonPath("$.departmentName", is("Engineering")));

        verify(analyticsService).getDepartmentSummary(3L, FROM, TO);
    }

    @Test
    void getDepartmentViolations_returns200AndDelegates() throws Exception {
        ViolationSummaryDTO dto = new ViolationSummaryDTO(
                5,
                2,
                3,
                List.of(new ViolationBreakdownDTO("TEMPERATURE", 5)),
                List.of()
        );

        when(analyticsService.getDepartmentViolationSummary(3L)).thenReturn(dto);

        mockMvc.perform(get("/api/analytics/departments/3/violations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", is(5)))
                .andExpect(jsonPath("$.active", is(2)));

        verify(analyticsService).getDepartmentViolationSummary(3L);
    }

    @Test
    void getDepartmentTrend_usesProvidedMetricAndDateRange() throws Exception {
        DepartmentTrendDTO dto = new DepartmentTrendDTO(
                Metric.HUMIDITY,
                "1h",
                List.of(new TrendPointDTO(FROM, 45.0))
        );

        when(analyticsService.getDepartmentTrend(3L, Metric.HUMIDITY, FROM, TO)).thenReturn(dto);

        mockMvc.perform(get("/api/analytics/departments/3/trends")
                        .param("metric", "HUMIDITY")
                        .param("from", "2026-05-18T10:00:00")
                        .param("to", "2026-05-18T12:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metric", is("HUMIDITY")))
                .andExpect(jsonPath("$.bucketSize", is("1h")));

        verify(analyticsService).getDepartmentTrend(3L, Metric.HUMIDITY, FROM, TO);
    }

    @Test
    void getCompanyDashboard_returns200AndDelegatesWithDateRange() throws Exception {
        CompanyDashboardDTO dto = new CompanyDashboardDTO(
                TO,
                12,
                40,
                3,
                List.of()
        );

        when(analyticsService.getCompanyDashboard(FROM, TO)).thenReturn(dto);

        mockMvc.perform(get("/api/analytics/company/dashboard")
                        .param("from", "2026-05-18T10:00:00")
                        .param("to", "2026-05-18T12:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRooms", is(12)))
                .andExpect(jsonPath("$.totalEmployees", is(40)));

        verify(analyticsService).getCompanyDashboard(FROM, TO);
    }

    @Test
    void getCompanyViolations_returns200AndDelegates() throws Exception {
        ViolationSummaryDTO dto = new ViolationSummaryDTO(
                8,
                3,
                5,
                List.of(new ViolationBreakdownDTO("HUMIDITY", 8)),
                List.of()
        );

        when(analyticsService.getCompanyViolationSummary()).thenReturn(dto);

        mockMvc.perform(get("/api/analytics/company/violations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", is(8)))
                .andExpect(jsonPath("$.resolved", is(5)));

        verify(analyticsService).getCompanyViolationSummary();
    }

    @Test
    void getCompanyTrend_defaultsMetricToTemperatureAndAllowsMissingDates() throws Exception {
        CompanyTrendDTO dto = new CompanyTrendDTO(
                Metric.TEMPERATURE,
                "1d",
                List.of(new TrendPointDTO(FROM, 22.0))
        );

        when(analyticsService.getCompanyTrend(Metric.TEMPERATURE, null, null)).thenReturn(dto);

        mockMvc.perform(get("/api/analytics/company/trends"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metric", is("TEMPERATURE")))
                .andExpect(jsonPath("$.bucketSize", is("1d")));

        verify(analyticsService).getCompanyTrend(Metric.TEMPERATURE, null, null);
    }
}

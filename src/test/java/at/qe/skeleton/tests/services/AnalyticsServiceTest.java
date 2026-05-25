package at.qe.skeleton.tests.services;

import at.qe.skeleton.common.exceptions.BadRequestException;
import at.qe.skeleton.dtos.*;
import at.qe.skeleton.models.*;
import at.qe.skeleton.repositories.MeasurementRepository;
import at.qe.skeleton.repositories.ThresholdViolationRepository;
import at.qe.skeleton.services.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
public class AnalyticsServiceTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2025, 1, 1, 10, 0);
    private static final LocalDateTime T1 = LocalDateTime.of(2025, 1, 1, 10, 5);

    private static final LocalDateTime FROM = LocalDateTime.of(2025, 1, 1, 0, 0);
    private static final LocalDateTime TO   = LocalDateTime.of(2025, 1, 1, 23, 59);

    @Mock
    private MeasurementService measurementService;

    @Mock
    private ThresholdViolationService thresholdViolationService;

    @Mock
    private RoomService roomService;

    @Mock
    private DepartmentService departmentService;

    @Mock
    private EmployeeProfileService employeeProfileService;

    @Mock
    private AuthenticatedUserService  authenticatedUserService;

    @Mock
    private MeasurementRepository measurementRepository;

    @Mock
    private ThresholdViolationRepository thresholdViolationRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    private Department department;
    private Room officeRoom;
    private Room commonRoom;
    private Userx buildingAdmin;
    private Userx departmentLead;
    private Userx employee;
    private EmployeeProfile employeeProfile;

    // Because we don't expose setters for some classes and for testing purposes
    // we brute force change fields via reflection...
    private static void setId(Object entity, Long id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set ID via reflection", e);
        }
    }

    @BeforeEach
    void setUp() {
        department = new Department();
        setId(department, 1L);
        department.setName("Engineering");

        officeRoom = new Room();
        setId(officeRoom, 10L);
        officeRoom.setName("Office A");
        officeRoom.setRoomType(RoomType.OFFICE);
        officeRoom.setActive(true);
        officeRoom.setPrivacyMode(false);
        officeRoom.setDepartment(department);

        commonRoom = new Room();
        setId(commonRoom, 20L);
        commonRoom.setName("Common Lounge");
        commonRoom.setRoomType(RoomType.COMMON_AREAS);
        commonRoom.setActive(true);
        commonRoom.setPrivacyMode(false);
        commonRoom.setDepartment(department);

        department.setRooms(new ArrayList<>(List.of(officeRoom, commonRoom)));

        buildingAdmin = new Userx();
        setId(buildingAdmin, 1L);
        buildingAdmin.setRoles(Set.of(UserxRole.BUILDING_ADMIN));

        departmentLead = new Userx();
        setId(departmentLead, 2L);
        departmentLead.setRoles(Set.of(UserxRole.DEPARTMENT_LEAD));
        department.setDepartmentLeader(departmentLead);

        employee = new Userx();
        setId(employee, 3L);
        employee.setRoles(Set.of(UserxRole.EMPLOYEE));

        employeeProfile = new EmployeeProfile();
        setId(employeeProfile, 3L);
        employeeProfile.setDepartment(department);
        employeeProfile.setRoom(officeRoom);
    }

    private Measurement measurement(Long roomId, Metric metric, float value, LocalDateTime ts) {
        Room room = roomService.getById(roomId);
        Measurement m = new Measurement();
        m.setRoom(room);
        m.setMetric(metric);
        m.setMeasurement(value);
        m.setTimestamp(ts);
        return m;
    }

    private ThresholdViolation violation(ViolationStatus status, Metric metric, Room room) {
        ThresholdViolation v = new ThresholdViolation();
        v.setViolationStatus(status);
        v.setMetric(metric);
        v.setRoom(room);
        return v;
    }

    @Test
    @DisplayName("getRoomSummary – BUILDING_ADMIN gets summary for any room")
    void getRoomSummary_buildingAdmin_returnsSummary() {
        Mockito.when(roomService.getById(10L)).thenReturn(officeRoom);
        Mockito.when(authenticatedUserService.getAuthenticatedUser()).thenReturn(buildingAdmin);

        List<Measurement> measurements = List.of(
                measurement(10L, Metric.TEMPERATURE, 22.0f, T0),
                measurement(10L, Metric.TEMPERATURE, 24.0f, T1)
        );
        Mockito.when(measurementService.getFiltered(eq(10L), isNull(), any(), any()))
                .thenReturn(measurements);

        RoomSummaryDTO result = analyticsService.getRoomSummary(10L, FROM, TO);

        assertThat(result).isNotNull();
        assertThat(result.roomId()).isEqualTo(10L);
        assertThat(result.roomName()).isEqualTo("Office A");
        assertThat(result.metrics()).containsKey(Metric.TEMPERATURE);

        MetricSummaryDTO tempStats = result.metrics().get(Metric.TEMPERATURE);
        assertThat(tempStats.avg()).isEqualTo(23.0);
        assertThat(tempStats.min()).isEqualTo(22.0);
        assertThat(tempStats.max()).isEqualTo(24.0);
        assertThat(tempStats.latest()).isEqualTo(24.0);
        assertThat(tempStats.count()).isEqualTo(2L);
    }

    @Test
    @DisplayName("getRoomSummary – EMPLOYEE views own office room successfully")
    void getRoomSummary_employeeOwnRoom_returnsSummary() {
        Mockito.when(roomService.getById(10L)).thenReturn(officeRoom);
        Mockito.when(authenticatedUserService.getAuthenticatedUser()).thenReturn(employee);
        Mockito.when(employeeProfileService.getMyProfile()).thenReturn(Optional.of(employeeProfile));
        Mockito.when(measurementService.getFiltered(eq(10L), isNull(), any(), any()))
                .thenReturn(List.of());

        RoomSummaryDTO result = analyticsService.getRoomSummary(10L, FROM, TO);

        assertThat(result).isNotNull();
        assertThat(result.roomId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("getRoomSummary – EMPLOYEE blocked from another department's room")
    void getRoomSummary_employeeWrongRoom_throwsAccessDenied() {
        Room otherRoom = new Room();
        setId(otherRoom, 99L);
        otherRoom.setRoomType(RoomType.OFFICE);
        otherRoom.setDepartment(new Department());

        Mockito.when(roomService.getById(99L)).thenReturn(otherRoom);
        Mockito.when(authenticatedUserService.getAuthenticatedUser()).thenReturn(employee);
        Mockito.when(employeeProfileService.getMyProfile()).thenReturn(Optional.of(employeeProfile));

        assertThatThrownBy(() -> analyticsService.getRoomSummary(99L, FROM, TO))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("getRoomSummary – EMPLOYEE blocked when privacy mode is active on own room")
    void getRoomSummary_employeePrivacyModeActive_throwsAccessDenied() {
        officeRoom.setPrivacyMode(true);

        Mockito.when(roomService.getById(10L)).thenReturn(officeRoom);
        Mockito.when(authenticatedUserService.getAuthenticatedUser()).thenReturn(employee);
        Mockito.when(employeeProfileService.getMyProfile()).thenReturn(Optional.of(employeeProfile));

        assertThatThrownBy(() -> analyticsService.getRoomSummary(10L, FROM, TO))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("minimum occupancy");
    }

    @Test
    @DisplayName("getRoomSummary – EMPLOYEE can access common area of own department")
    void getRoomSummary_employeeCommonArea_returnsSummary() {
        Mockito.when(roomService.getById(20L)).thenReturn(commonRoom);
        Mockito.when(authenticatedUserService.getAuthenticatedUser()).thenReturn(employee);
        Mockito.when(employeeProfileService.getMyProfile()).thenReturn(Optional.of(employeeProfile));
        Mockito.when(measurementService.getFiltered(eq(20L), isNull(), any(), any()))
                .thenReturn(List.of());

        RoomSummaryDTO result = analyticsService.getRoomSummary(20L, FROM, TO);

        assertThat(result).isNotNull();
        assertThat(result.roomId()).isEqualTo(20L);
    }

    @Test
    @DisplayName("getRoomSummary – DEPARTMENT_LEAD can access any room in their department")
    void getRoomSummary_departmentLeadOwnDept_returnsSummary() {
        Mockito.when(roomService.getById(10L)).thenReturn(officeRoom);
        Mockito.when(authenticatedUserService.getAuthenticatedUser()).thenReturn(departmentLead);
        Mockito.when(measurementService.getFiltered(eq(10L), isNull(), any(), any()))
                .thenReturn(List.of());

        RoomSummaryDTO result = analyticsService.getRoomSummary(10L, FROM, TO);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getRoomSummary – DEPARTMENT_LEAD blocked from room outside their department")
    void getRoomSummary_departmentLeadWrongDept_throwsAccessDenied() {
        Department other = new Department();
        setId(other, 99L);
        other.setDepartmentLeader(buildingAdmin);

        Room foreignRoom = new Room();
        setId(foreignRoom, 77L);
        foreignRoom.setRoomType(RoomType.OFFICE);
        foreignRoom.setDepartment(other);

        Mockito.when(roomService.getById(77L)).thenReturn(foreignRoom);
        Mockito.when(authenticatedUserService.getAuthenticatedUser()).thenReturn(departmentLead);

        assertThatThrownBy(() -> analyticsService.getRoomSummary(77L, FROM, TO))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("getRoomSummary – invalid date range throws BadRequestException")
    void getRoomSummary_invalidRange_throwsBadRequest() {
        Mockito.when(roomService.getById(10L)).thenReturn(officeRoom);
        Mockito.when(authenticatedUserService.getAuthenticatedUser()).thenReturn(buildingAdmin);

        assertThatThrownBy(() -> analyticsService.getRoomSummary(10L, TO, FROM))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("getRoomSummary – null from/to defaults to last 24 hours")
    void getRoomSummary_nullFromTo_defaultsToLast24Hours() {
        Mockito.when(roomService.getById(10L)).thenReturn(officeRoom);
        Mockito.when(authenticatedUserService.getAuthenticatedUser()).thenReturn(buildingAdmin);
        Mockito.when(measurementService.getFiltered(eq(10L), isNull(), any(), any()))
                .thenReturn(List.of());

        RoomSummaryDTO result = analyticsService.getRoomSummary(10L, null, null);

        assertThat(result).isNotNull();
        Mockito.verify(measurementService).getFiltered(eq(10L), isNull(), any(), any());
    }

    @Test
    @DisplayName("getRoomSummary – no measurements returns empty metric stats with zero count")
    void getRoomSummary_noMeasurements_returnsNullValuesWithZeroCount() {
        Mockito.when(roomService.getById(10L)).thenReturn(officeRoom);
        Mockito.when(authenticatedUserService.getAuthenticatedUser()).thenReturn(buildingAdmin);
        Mockito.when(measurementService.getFiltered(eq(10L), isNull(), any(), any()))
                .thenReturn(List.of());

        RoomSummaryDTO result = analyticsService.getRoomSummary(10L, FROM, TO);

        MetricSummaryDTO tempStats = result.metrics().get(Metric.TEMPERATURE);
        assertThat(tempStats.count()).isZero();
        assertThat(tempStats.avg()).isNull();
        assertThat(tempStats.min()).isNull();
        assertThat(tempStats.max()).isNull();
        assertThat(tempStats.latest()).isNull();
    }

    @Test
    @DisplayName("getRoomSummary – EMPLOYEE without profile throws AccessDeniedException")
    void getRoomSummary_employeeNoProfile_throwsAccessDenied() {
        Mockito.when(roomService.getById(10L)).thenReturn(officeRoom);
        Mockito.when(authenticatedUserService.getAuthenticatedUser()).thenReturn(employee);
        Mockito.when(employeeProfileService.getMyProfile()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> analyticsService.getRoomSummary(10L, FROM, TO))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("No employee profile found");
    }

    @Test
    @DisplayName("getRoomTrend – BUILDING_ADMIN gets raw trend for short window")
    void getRoomTrend_buildingAdminShortWindow_returnsRawTrend() {
        Mockito.when(roomService.getById(10L)).thenReturn(officeRoom);
        Mockito.when(authenticatedUserService.getAuthenticatedUser()).thenReturn(buildingAdmin);

        List<Measurement> measurements = List.of(
                measurement(10L, Metric.TEMPERATURE, 21.5f, T0),
                measurement(10L, Metric.TEMPERATURE, 22.0f, T1)
        );
        Mockito.when(measurementService.getFiltered(eq(10L), eq(Metric.TEMPERATURE), any(), any()))
                .thenReturn(measurements);

        RoomTrendDTO result = analyticsService.getRoomTrend(10L, Metric.TEMPERATURE, FROM, TO);

        assertThat(result).isNotNull();
        assertThat(result.bucketSize()).isEqualTo("raw");
        assertThat(result.granularityReduced()).isFalse();
        assertThat(result.points()).hasSize(2);
    }

    @Test
    @DisplayName("getRoomTrend – DEPARTMENT_LEAD on occupied office forces daily granularity")
    void getRoomTrend_departmentLeadOccupiedOffice_forcesDailyBucket() {
        officeRoom.setPrivacyMode(false);

        Mockito.when(roomService.getById(10L)).thenReturn(officeRoom);
        Mockito.when(authenticatedUserService.getAuthenticatedUser()).thenReturn(departmentLead);
        Mockito.when(measurementService.getFiltered(eq(10L), eq(Metric.TEMPERATURE), any(), any()))
                .thenReturn(List.of());

        LocalDateTime from = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime to   = LocalDateTime.of(2025, 1, 10, 0, 0);

        RoomTrendDTO result = analyticsService.getRoomTrend(10L, Metric.TEMPERATURE, from, to);

        assertThat(result.granularityReduced()).isTrue();
        assertThat(result.bucketSize()).isEqualTo("1d");
    }

    @Test
    @DisplayName("getRoomTrend – DEPARTMENT_LEAD on common area gets full granularity")
    void getRoomTrend_departmentLeadCommonArea_fullGranularity() {
        Mockito.when(roomService.getById(20L)).thenReturn(commonRoom);
        Mockito.when(authenticatedUserService.getAuthenticatedUser()).thenReturn(departmentLead);
        Mockito.when(measurementService.getFiltered(eq(20L), eq(Metric.HUMIDITY), any(), any()))
                .thenReturn(List.of());

        RoomTrendDTO result = analyticsService.getRoomTrend(20L, Metric.HUMIDITY, FROM, TO);

        assertThat(result.granularityReduced()).isFalse();
    }

    @Test
    @DisplayName("getRoomTrend – DEPARTMENT_LEAD on office with privacy mode ON does not force reduced granularity")
    void getRoomTrend_departmentLeadOfficePrivacyModeOn_notForceReduced() {
        officeRoom.setPrivacyMode(true);

        Mockito.when(roomService.getById(10L)).thenReturn(officeRoom);
        Mockito.when(authenticatedUserService.getAuthenticatedUser()).thenReturn(departmentLead);
        Mockito.when(measurementService.getFiltered(eq(10L), eq(Metric.TEMPERATURE), any(), any()))
                .thenReturn(List.of());

        RoomTrendDTO result = analyticsService.getRoomTrend(10L, Metric.TEMPERATURE, FROM, TO);

        assertThat(result.granularityReduced()).isFalse();
    }

    @Test
    @DisplayName("getRoomTrend – buckets correctly average measurements in same window")
    void getRoomTrend_bucketsAverageMeasurementsCorrectly() {
        Mockito.when(roomService.getById(10L)).thenReturn(officeRoom);
        Mockito.when(authenticatedUserService.getAuthenticatedUser()).thenReturn(buildingAdmin);

        LocalDateTime ts1 = LocalDateTime.of(2025, 1, 1, 0, 10);
        LocalDateTime ts2 = LocalDateTime.of(2025, 1, 1, 0, 50);

        LocalDateTime from = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime to   = LocalDateTime.of(2025, 2, 1, 0, 0);

        List<Measurement> measurements = List.of(
                measurement(10L, Metric.TEMPERATURE, 20.0f, ts1),
                measurement(10L, Metric.TEMPERATURE, 22.0f, ts2)
        );
        Mockito.when(measurementService.getFiltered(eq(10L), eq(Metric.TEMPERATURE), any(), any()))
                .thenReturn(measurements);

        RoomTrendDTO result = analyticsService.getRoomTrend(10L, Metric.TEMPERATURE, from, to);

        assertThat(result.bucketSize()).isEqualTo("1h");
        assertThat(result.points()).hasSize(1);
        assertThat(result.points().getFirst().value()).isEqualTo(21.0);
    }

    @Test
    @DisplayName("getRoomTrend – invalid range throws BadRequestException")
    void getRoomTrend_invalidRange_throwsBadRequest() {
        Mockito.when(roomService.getById(10L)).thenReturn(officeRoom);
        Mockito.when(authenticatedUserService.getAuthenticatedUser()).thenReturn(buildingAdmin);

        assertThatThrownBy(() -> analyticsService.getRoomTrend(10L, Metric.TEMPERATURE, TO, FROM))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("getRoomViolationSummary – returns correct counts and breakdown")
    void getRoomViolationSummary_returnsCorrectCounts() {
        Mockito.when(roomService.getById(10L)).thenReturn(officeRoom);
        Mockito.when(authenticatedUserService.getAuthenticatedUser()).thenReturn(buildingAdmin);

        List<ThresholdViolation> violations = List.of(
                violation(ViolationStatus.ACTIVE,   Metric.TEMPERATURE, officeRoom),
                violation(ViolationStatus.ACTIVE,   Metric.IAQ,         officeRoom),
                violation(ViolationStatus.RESOLVED, Metric.TEMPERATURE, officeRoom)
        );
        Mockito.when(thresholdViolationService.findAll(isNull(), eq(10L), isNull()))
                .thenReturn(violations);

        RoomViolationSummaryDTO result = analyticsService.getRoomViolationSummary(10L);

        assertThat(result.total()).isEqualTo(3);
        assertThat(result.active()).isEqualTo(2);
        assertThat(result.resolved()).isEqualTo(1);
        assertThat(result.byMetric())
                .anyMatch(b -> b.label().equals(Metric.TEMPERATURE.name()) && b.count() == 2)
                .anyMatch(b -> b.label().equals(Metric.IAQ.name())         && b.count() == 1);
    }

    @Test
    @DisplayName("getRoomViolationSummary – no violations returns zero counts")
    void getRoomViolationSummary_noViolations_returnsZeroCounts() {
        Mockito.when(roomService.getById(10L)).thenReturn(officeRoom);
        Mockito.when(authenticatedUserService.getAuthenticatedUser()).thenReturn(buildingAdmin);
        Mockito.when(thresholdViolationService.findAll(isNull(), eq(10L), isNull()))
                .thenReturn(List.of());

        RoomViolationSummaryDTO result = analyticsService.getRoomViolationSummary(10L);

        assertThat(result.total()).isZero();
        assertThat(result.active()).isZero();
        assertThat(result.resolved()).isZero();
        assertThat(result.byMetric()).isEmpty();
    }

    @Test
    @DisplayName("getRoomViolationSummary – EMPLOYEE blocked by privacy mode")
    void getRoomViolationSummary_employeePrivacyMode_throwsAccessDenied() {
        officeRoom.setPrivacyMode(true);
        Mockito.when(roomService.getById(10L)).thenReturn(officeRoom);
        Mockito.when(authenticatedUserService.getAuthenticatedUser()).thenReturn(employee);
        Mockito.when(employeeProfileService.getMyProfile()).thenReturn(Optional.of(employeeProfile));

        assertThatThrownBy(() -> analyticsService.getRoomViolationSummary(10L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("getDepartmentSummary – DEPARTMENT_LEAD views own department successfully")
    void getDepartmentSummary_departmentLeadOwnDept_returnsSummary() {
        Mockito.when(departmentService.getDepartmentById(1L)).thenReturn(department);
        Mockito.when(authenticatedUserService.getAuthenticatedUser()).thenReturn(departmentLead);

        stubMeasurementsForRoom(officeRoom.getId(), List.of());
        stubMeasurementsForRoom(commonRoom.getId(), List.of());
        Mockito.when(thresholdViolationService.findAll(eq(ViolationStatus.ACTIVE), anyLong(), isNull()))
                .thenReturn(List.of());

        DepartmentAnalyticsDTO result = analyticsService.getDepartmentSummary(1L, FROM, TO);

        assertThat(result).isNotNull();
        assertThat(result.departmentId()).isEqualTo(1L);
        assertThat(result.departmentName()).isEqualTo("Engineering");
        assertThat(result.roomCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("getDepartmentSummary – DEPARTMENT_LEAD blocked from another department")
    void getDepartmentSummary_departmentLeadWrongDept_throwsAccessDenied() {
        Department other = new Department();
        setId(other, 99L);
        other.setDepartmentLeader(buildingAdmin);

        Mockito.when(departmentService.getDepartmentById(99L)).thenReturn(other);
        Mockito.when(authenticatedUserService.getAuthenticatedUser()).thenReturn(departmentLead);

        assertThatThrownBy(() -> analyticsService.getDepartmentSummary(99L, FROM, TO))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("getDepartmentSummary – MANAGEMENT can access any department")
    void getDepartmentSummary_managementAnyDept_returnsSummary() {
        Userx management = new Userx();
        setId(management, 5L);
        management.setRoles(Set.of(UserxRole.MANAGEMENT));

        Mockito.when(departmentService.getDepartmentById(1L)).thenReturn(department);
        Mockito.when(authenticatedUserService.getAuthenticatedUser()).thenReturn(management);

        stubMeasurementsForRoom(officeRoom.getId(), List.of());
        stubMeasurementsForRoom(commonRoom.getId(), List.of());
        Mockito.when(thresholdViolationService.findAll(eq(ViolationStatus.ACTIVE), anyLong(), isNull()))
                .thenReturn(List.of());

        DepartmentAnalyticsDTO result = analyticsService.getDepartmentSummary(1L, FROM, TO);

        assertThat(result).isNotNull();
        assertThat(result.departmentId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getDepartmentSummary – sums active violations across all rooms")
    void getDepartmentSummary_sumsActiveViolationsAcrossRooms() {
        Mockito.when(departmentService.getDepartmentById(1L)).thenReturn(department);
        Mockito.when(authenticatedUserService.getAuthenticatedUser()).thenReturn(departmentLead);

        stubMeasurementsForRoom(officeRoom.getId(), List.of());
        stubMeasurementsForRoom(commonRoom.getId(), List.of());

        Mockito.when(thresholdViolationService.findAll(eq(ViolationStatus.ACTIVE), eq(officeRoom.getId()), isNull()))
                .thenReturn(List.of(violation(ViolationStatus.ACTIVE, Metric.IAQ, officeRoom)));
        Mockito.when(thresholdViolationService.findAll(eq(ViolationStatus.ACTIVE), eq(commonRoom.getId()), isNull()))
                .thenReturn(List.of(
                        violation(ViolationStatus.ACTIVE, Metric.HUMIDITY, commonRoom),
                        violation(ViolationStatus.ACTIVE, Metric.HUMIDITY, commonRoom)
                ));

        DepartmentAnalyticsDTO result = analyticsService.getDepartmentSummary(1L, FROM, TO);

        assertThat(result.activeViolations()).isEqualTo(3);
    }

    @Test
    @DisplayName("getDepartmentSummary – only active rooms are included")
    void getDepartmentSummary_inactiveRoomsExcluded() {
        Room inactiveRoom = new Room();
        setId(inactiveRoom, 30L);
        inactiveRoom.setActive(false);
        inactiveRoom.setDepartment(department);
        department.getRooms().add(inactiveRoom);

        Mockito.when(departmentService.getDepartmentById(1L)).thenReturn(department);
        Mockito.when(authenticatedUserService.getAuthenticatedUser()).thenReturn(departmentLead);

        stubMeasurementsForRoom(officeRoom.getId(), List.of());
        stubMeasurementsForRoom(commonRoom.getId(), List.of());
        Mockito.when(thresholdViolationService.findAll(eq(ViolationStatus.ACTIVE), anyLong(), isNull()))
                .thenReturn(List.of());

        DepartmentAnalyticsDTO result = analyticsService.getDepartmentSummary(1L, FROM, TO);

        assertThat(result.roomCount()).isEqualTo(2);
        Mockito.verify(thresholdViolationService, Mockito.never())
                .findAll(any(), eq(30L), any());
    }

    @Test
    @DisplayName("getDepartmentTrend – returns trend with correct metric and bucket")
    void getDepartmentTrend_returnsTrend() {
        Mockito.when(departmentService.getDepartmentById(1L)).thenReturn(department);
        Mockito.when(authenticatedUserService.getAuthenticatedUser()).thenReturn(departmentLead);

        LocalDateTime from = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime to   = LocalDateTime.of(2025, 1, 9, 0, 0);

        stubMeasurementsForRoom(officeRoom.getId(), List.of(
                measurement(officeRoom.getId(), Metric.TEMPERATURE, 22.0f, from.plusMinutes(10))
        ));
        stubMeasurementsForRoom(commonRoom.getId(), List.of());

        DepartmentTrendDTO result = analyticsService.getDepartmentTrend(1L, Metric.TEMPERATURE, from, to);

        assertThat(result).isNotNull();
        assertThat(result.metric()).isEqualTo(Metric.TEMPERATURE);
        assertThat(result.bucketSize()).isEqualTo("1h");
    }

    @Test
    @DisplayName("getDepartmentTrend – DEPARTMENT_LEAD blocked from other department")
    void getDepartmentTrend_departmentLeadWrongDept_throwsAccessDenied() {
        Department other = new Department();
        setId(other, 99L);
        other.setDepartmentLeader(buildingAdmin);

        Mockito.when(departmentService.getDepartmentById(99L)).thenReturn(other);
        Mockito.when(authenticatedUserService.getAuthenticatedUser()).thenReturn(departmentLead);

        assertThatThrownBy(() -> analyticsService.getDepartmentTrend(99L, Metric.TEMPERATURE, FROM, TO))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("getDepartmentTrend – room-fair: rooms without data in a bucket do not bias average")
    void getDepartmentTrend_roomFairAveraging() {
        Mockito.when(departmentService.getDepartmentById(1L)).thenReturn(department);
        Mockito.when(authenticatedUserService.getAuthenticatedUser()).thenReturn(departmentLead);

        LocalDateTime from = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime to   = LocalDateTime.of(2025, 1, 9, 0, 0);

        Mockito.when(measurementService.getFiltered(anyLong(), eq(Metric.TEMPERATURE), any(), any()))
                .thenAnswer(invocation -> {
                    Long roomId = invocation.getArgument(0);

                    if (roomId.equals(officeRoom.getId())) {
                        return List.of(measurement(roomId, Metric.TEMPERATURE, 30.0f, from.plusMinutes(30)));
                    }
                    return List.of();
                });

        DepartmentTrendDTO result = analyticsService.getDepartmentTrend(1L, Metric.TEMPERATURE, from, to);

        assertThat(result.points()).hasSize(1);
        assertThat(result.points().getFirst().value()).isEqualTo(30.0);
    }

    @Test
    @DisplayName("getDepartmentViolationSummary – MANAGEMENT gets anonymized summary (no room breakdown)")
    void getDepartmentViolationSummary_management_anonymized() {
        Userx management = new Userx();
        setId(management, 5L);
        management.setRoles(Set.of(UserxRole.MANAGEMENT));

        Mockito.when(departmentService.getDepartmentById(1L)).thenReturn(department);
        Mockito.when(authenticatedUserService.getAuthenticatedUser()).thenReturn(management);

        List<ThresholdViolation> violations = List.of(
                violation(ViolationStatus.ACTIVE,   Metric.TEMPERATURE, officeRoom),
                violation(ViolationStatus.RESOLVED, Metric.IAQ,         commonRoom)
        );
        Mockito.when(thresholdViolationService.findAll(isNull(), isNull(), eq(1L)))
                .thenReturn(violations);

        ViolationSummaryDTO result = analyticsService.getDepartmentViolationSummary(1L);

        assertThat(result.total()).isEqualTo(2);
        assertThat(result.byRoom()).isEmpty();
    }

    @Test
    @DisplayName("getDepartmentViolationSummary – BUILDING_ADMIN gets full room-level breakdown")
    void getDepartmentViolationSummary_buildingAdmin_fullBreakdown() {
        Mockito.when(departmentService.getDepartmentById(1L)).thenReturn(department);
        Mockito.when(authenticatedUserService.getAuthenticatedUser()).thenReturn(buildingAdmin);

        List<ThresholdViolation> violations = List.of(
                violation(ViolationStatus.ACTIVE, Metric.TEMPERATURE, officeRoom),
                violation(ViolationStatus.ACTIVE, Metric.IAQ,         officeRoom)
        );
        Mockito.when(thresholdViolationService.findAll(isNull(), isNull(), eq(1L)))
                .thenReturn(violations);

        ViolationSummaryDTO result = analyticsService.getDepartmentViolationSummary(1L);

        assertThat(result.byRoom()).isNotEmpty();
        assertThat(result.byRoom())
                .anyMatch(b -> b.label().equals("Office A") && b.count() == 2);
    }

    @Test
    @DisplayName("getDepartmentViolationSummary – DEPARTMENT_LEAD not blocked from own department")
    void getDepartmentViolationSummary_departmentLeadOwnDept_DoesNotthrowAccessDenied() {
        Mockito.when(departmentService.getDepartmentById(1L)).thenReturn(department);
        Mockito.when(authenticatedUserService.getAuthenticatedUser()).thenReturn(departmentLead);

        assertThatCode(() -> analyticsService.getDepartmentViolationSummary(1L))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("getDepartmentViolationSummary – DEPARTMENT_LEAD blocked from viewing other department")
    void getDepartmentViolationSummary_departmentLeadOtherDept_throwsAccessDenied() {
        Mockito.when(departmentService.getDepartmentById(2L)).thenReturn(new Department());
        Mockito.when(authenticatedUserService.getAuthenticatedUser()).thenReturn(departmentLead);

        assertThatThrownBy(() -> analyticsService.getDepartmentViolationSummary(2L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("getCompanyDashboard – BUILDING_ADMIN gets aggregated company dashboard")
    void getCompanyDashboard_buildingAdmin_returnsCompanyDashboard() {
        Mockito.when(departmentService.getAll()).thenReturn(List.of(department));
        Mockito.when(employeeProfileService.getAll(isNull(), isNull())).thenReturn(List.of(employeeProfile));

        stubMeasurementsForRoom(officeRoom.getId(), List.of());
        stubMeasurementsForRoom(commonRoom.getId(), List.of());
        Mockito.when(thresholdViolationService.findAll(eq(ViolationStatus.ACTIVE), anyLong(), isNull()))
                .thenReturn(List.of());

        CompanyDashboardDTO result = analyticsService.getCompanyDashboard(FROM, TO);

        assertThat(result).isNotNull();
        assertThat(result.totalRooms()).isEqualTo(2);
        assertThat(result.totalEmployees()).isEqualTo(1);
        assertThat(result.activeViolations()).isZero();
        assertThat(result.departments()).hasSize(1);
        assertThat(result.departments().getFirst().departmentId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getCompanyDashboard – sums active violations across departments")
    void getCompanyDashboard_sumsActiveViolations() {
        Mockito.when(departmentService.getAll()).thenReturn(List.of(department));
        Mockito.when(employeeProfileService.getAll(isNull(), isNull())).thenReturn(List.of());

        stubMeasurementsForRoom(officeRoom.getId(), List.of());
        stubMeasurementsForRoom(commonRoom.getId(), List.of());

        Mockito.when(thresholdViolationService.findAll(eq(ViolationStatus.ACTIVE), eq(officeRoom.getId()), isNull()))
                .thenReturn(List.of(violation(ViolationStatus.ACTIVE, Metric.IAQ, officeRoom)));
        Mockito.when(thresholdViolationService.findAll(eq(ViolationStatus.ACTIVE), eq(commonRoom.getId()), isNull()))
                .thenReturn(List.of());

        CompanyDashboardDTO result = analyticsService.getCompanyDashboard(FROM, TO);

        assertThat(result.activeViolations()).isEqualTo(1);
    }

    @Test
    @DisplayName("getCompanyDashboard – invalid range throws BadRequestException")
    void getCompanyDashboard_invalidRange_throwsBadRequest() {
        assertThatThrownBy(() -> analyticsService.getCompanyDashboard(TO, FROM))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("getCompanyDashboard – null from/to defaults to last 7 days")
    void getCompanyDashboard_nullFromTo_defaultsToLast7Days() {
        Mockito.when(departmentService.getAll()).thenReturn(List.of());

        CompanyDashboardDTO result = analyticsService.getCompanyDashboard(null, null);

        assertThat(result).isNotNull();
        assertThat(result.totalRooms()).isZero();
    }

    @Test
    @DisplayName("getCompanyViolationSummary – MANAGEMENT gets anonymized (no room breakdown)")
    void getCompanyViolationSummary_management_anonymized() {
        Userx management = new Userx();
        setId(management, 5L);
        management.setRoles(Set.of(UserxRole.MANAGEMENT));

        Mockito.when(authenticatedUserService.getAuthenticatedUser()).thenReturn(management);
        Mockito.when(departmentService.getAll()).thenReturn(List.of(department));

        List<ThresholdViolation> violations = List.of(
                violation(ViolationStatus.ACTIVE,   Metric.TEMPERATURE, officeRoom),
                violation(ViolationStatus.RESOLVED, Metric.IAQ,         officeRoom)
        );
        Mockito.when(thresholdViolationService.findAll(isNull(), isNull(), isNull()))
                .thenReturn(violations);

        ViolationSummaryDTO result = analyticsService.getCompanyViolationSummary();

        assertThat(result.total()).isEqualTo(2);
        assertThat(result.active()).isEqualTo(1);
        assertThat(result.resolved()).isEqualTo(1);
        assertThat(result.byRoom()).isEmpty();
    }

    @Test
    @DisplayName("getCompanyViolationSummary – BUILDING_ADMIN gets room-level breakdown")
    void getCompanyViolationSummary_buildingAdmin_roomBreakdown() {
        Mockito.when(authenticatedUserService.getAuthenticatedUser()).thenReturn(buildingAdmin);
        Mockito.when(departmentService.getAll()).thenReturn(List.of(department));

        List<ThresholdViolation> violations = List.of(
                violation(ViolationStatus.ACTIVE, Metric.TEMPERATURE, officeRoom)
        );
        Mockito.when(thresholdViolationService.findAll(isNull(), isNull(), isNull()))
                .thenReturn(violations);

        ViolationSummaryDTO result = analyticsService.getCompanyViolationSummary();

        assertThat(result.byRoom()).isNotEmpty();
        assertThat(result.byRoom().getFirst().label()).isEqualTo("Office A");
        assertThat(result.byRoom().getFirst().count()).isEqualTo(1);
    }

    @Test
    @DisplayName("getCompanyTrend – BUILDING_ADMIN gets aggregated trend across all rooms")
    void getCompanyTrend_buildingAdmin_returnsAggregatedTrend() {
        Mockito.when(departmentService.getAll()).thenReturn(List.of(department));

        LocalDateTime from = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime to   = LocalDateTime.of(2025, 1, 9, 0, 0);

        Mockito.when(measurementService.getFiltered(anyLong(), eq(Metric.IAQ), any(), any()))
                .thenAnswer(invocation -> {
                    Long roomId = invocation.getArgument(0);

                    if (roomId.equals(officeRoom.getId())) {
                        return List.of(measurement(roomId, Metric.IAQ, 20.0f, from.plusMinutes(10)));
                    }
                    if (roomId.equals(commonRoom.getId())) {
                        return List.of(measurement(roomId, Metric.IAQ, 24.0f, from.plusMinutes(10)));
                    }
                    return List.of();
                });

        CompanyTrendDTO result = analyticsService.getCompanyTrend(Metric.IAQ, from, to);

        assertThat(result).isNotNull();
        assertThat(result.metric()).isEqualTo(Metric.IAQ);
        assertThat(result.points()).hasSize(1);
        assertThat(result.points().getFirst().value()).isEqualTo(22.0);
    }

    @Test
    @DisplayName("getCompanyTrend – empty departments returns empty points")
    void getCompanyTrend_noDepartments_returnsEmptyPoints() {
        Mockito.when(departmentService.getAll()).thenReturn(List.of());
        CompanyTrendDTO result = analyticsService.getCompanyTrend(
                Metric.TEMPERATURE,
                LocalDateTime.of(2025, 1, 1, 0, 0),
                LocalDateTime.of(2025, 1, 9, 0, 0));

        assertThat(result.points()).isEmpty();
    }

    @Test
    @DisplayName("getCompanyTrend – invalid range throws BadRequestException")
    void getCompanyTrend_invalidRange_throwsBadRequest() {
        assertThatThrownBy(() -> analyticsService.getCompanyTrend(Metric.TEMPERATURE, TO, FROM))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("resolveBucketSize – window ≤ 7 days returns raw")
    void resolveBucketSize_sevenDayWindow_returnsRaw() {
        Mockito.when(roomService.getById(10L)).thenReturn(officeRoom);
        Mockito.when(authenticatedUserService.getAuthenticatedUser()).thenReturn(buildingAdmin);
        Mockito.when(measurementService.getFiltered(any(), any(), any(), any())).thenReturn(List.of());

        LocalDateTime from = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime to   = LocalDateTime.of(2025, 1, 7, 23, 59);

        RoomTrendDTO result = analyticsService.getRoomTrend(10L, Metric.TEMPERATURE, from, to);
        assertThat(result.bucketSize()).isEqualTo("raw");
    }

    @Test
    @DisplayName("resolveBucketSize – window > 7 days and ≤ 90 days returns 1h")
    void resolveBucketSize_thirtyDayWindow_returns1h() {
        Mockito.when(roomService.getById(10L)).thenReturn(officeRoom);
        Mockito.when(authenticatedUserService.getAuthenticatedUser()).thenReturn(buildingAdmin);
        Mockito.when(measurementService.getFiltered(any(), any(), any(), any())).thenReturn(List.of());

        LocalDateTime from = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime to   = LocalDateTime.of(2025, 1, 31, 0, 0);

        RoomTrendDTO result = analyticsService.getRoomTrend(10L, Metric.TEMPERATURE, from, to);
        assertThat(result.bucketSize()).isEqualTo("1h");
    }

    @Test
    @DisplayName("resolveBucketSize – window > 90 days and ≤ 365 days returns 6h")
    void resolveBucketSize_ninetyDayWindow_returns6h() {
        Mockito.when(roomService.getById(10L)).thenReturn(officeRoom);
        Mockito.when(authenticatedUserService.getAuthenticatedUser()).thenReturn(buildingAdmin);
        Mockito.when(measurementService.getFiltered(any(), any(), any(), any())).thenReturn(List.of());

        LocalDateTime from = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime to   = LocalDateTime.of(2025, 6, 1, 0, 0);

        RoomTrendDTO result = analyticsService.getRoomTrend(10L, Metric.TEMPERATURE, from, to);
        assertThat(result.bucketSize()).isEqualTo("6h");
    }

    @Test
    @DisplayName("resolveBucketSize – window > 365 days returns 1d")
    void resolveBucketSize_overYearWindow_returns1d() {
        Mockito.when(roomService.getById(10L)).thenReturn(officeRoom);
        Mockito.when(authenticatedUserService.getAuthenticatedUser()).thenReturn(buildingAdmin);
        Mockito.when(measurementService.getFiltered(any(), any(), any(), any())).thenReturn(List.of());

        LocalDateTime from = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime to   = LocalDateTime.of(2025, 6, 1, 0, 0);

        RoomTrendDTO result = analyticsService.getRoomTrend(10L, Metric.TEMPERATURE, from, to);
        assertThat(result.bucketSize()).isEqualTo("1d");
    }

    @Test
    @DisplayName("Metric values are rounded to 2 decimal places")
    void metricValues_roundedToTwoDecimalPlaces() {
        Mockito.when(roomService.getById(10L)).thenReturn(officeRoom);
        Mockito.when(authenticatedUserService.getAuthenticatedUser()).thenReturn(buildingAdmin);

        List<Measurement> measurements = List.of(
                measurement(10L, Metric.TEMPERATURE, 22.333f, T0),
                measurement(10L, Metric.TEMPERATURE, 22.666f, T1)
        );
        Mockito.when(measurementService.getFiltered(eq(10L), isNull(), any(), any()))
                .thenReturn(measurements);

        RoomSummaryDTO result = analyticsService.getRoomSummary(10L, FROM, TO);

        MetricSummaryDTO stats = result.metrics().get(Metric.TEMPERATURE);
        assertThat(stats.avg()).isLessThanOrEqualTo(22.51);
        assertThat(stats.avg()).isGreaterThanOrEqualTo(22.49);
    }

    private void stubMeasurementsForRoom(Long roomId, List<Measurement> measurements) {
        Mockito.when(measurementService.getFiltered(eq(roomId), any(), any(), any()))
                .thenReturn(measurements);
    }
}

package at.qe.skeleton.services;

import at.qe.skeleton.dtos.*;
import at.qe.skeleton.models.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AnalyticsService {

    private final MeasurementService measurementService;
    private final ThresholdViolationService thresholdViolationService;
    private final RoomService roomService;
    private final DepartmentService departmentService;
    private final EmployeeProfileService employeeProfileService;

    public AnalyticsService(
            MeasurementService measurementService,
            ThresholdViolationService thresholdViolationService,
            RoomService roomService,
            DepartmentService departmentService,
            EmployeeProfileService employeeProfileService) {
        this.measurementService = measurementService;
        this.thresholdViolationService = thresholdViolationService;
        this.roomService = roomService;
        this.departmentService = departmentService;
        this.employeeProfileService = employeeProfileService;
    }

    @PreAuthorize("isAuthenticated()")
    public RoomSummaryDTO getRoomSummary(Long roomId) {
        Room room = roomService.getById(roomId);

        List<Measurement> measurements = measurementService.getFiltered(roomId, null, null, null);

        Map<Metric, MetricSummaryDTO> metricStats = Arrays.stream(Metric.values())
                .collect(Collectors.toMap(
                        metric -> metric,
                        metric -> buildMetricSummary(measurements, metric)
                ));

        log.debug("Generated room summary for roomId={}", roomId);
        return new RoomSummaryDTO(roomId, room.getName(), LocalDateTime.now(), metricStats);
    }

    @PreAuthorize("isAuthenticated()")
    public RoomTrendDTO getRoomTrend(Long roomId, Metric metric, LocalDateTime from, LocalDateTime to) {
        roomService.getById(roomId);

        LocalDateTime effectiveTo   = to   != null ? to   : LocalDateTime.now();
        LocalDateTime effectiveFrom = from != null ? from : effectiveTo.minusHours(24);

        List<Measurement> measurements = measurementService.getFiltered(roomId, metric, effectiveFrom, effectiveTo);

        String bucketSize = resolveBucketSize(effectiveFrom, effectiveTo);
        List<TrendPointDTO> points = bucketize(measurements, effectiveFrom, effectiveTo, bucketSize);

        log.debug("Generated room trend: roomId={}, metric={}, from={}, to={}, bucketSize={}, points={}",
                roomId, metric, effectiveFrom, effectiveTo, bucketSize, points.size());
        return new RoomTrendDTO(roomId, metric, bucketSize, points);
    }

    @PreAuthorize("isAuthenticated()")
    public DepartmentAnalyticsDTO getDepartmentSummary(Long departmentId) {
        Department department = departmentService.getDepartmentById(departmentId);

        List<Room> rooms = department.getRooms().stream()
                .filter(Room::isActive)
                .toList();

        int activeViolations = rooms.stream()
                .mapToInt(r -> thresholdViolationService
                        .findAll(ViolationStatus.ACTIVE, r.getId(), null)
                        .size())
                .sum();

        Map<Metric, Double> avgMetrics = computeAvgMetricsForRooms(rooms);

        log.debug("Generated department summary for departmentId={}", departmentId);
        return new DepartmentAnalyticsDTO(
                departmentId,
                department.getName(),
                rooms.size(),
                activeViolations,
                avgMetrics
        );
    }

    @PreAuthorize("isAuthenticated()")
    public ViolationSummaryDTO getDepartmentViolationSummary(Long departmentId) {
        Department department = departmentService.getDepartmentById(departmentId);

        List<Room> rooms = department.getRooms().stream()
                .filter(Room::isActive)
                .toList();
        List<Long> roomIds = rooms.stream().map(Room::getId).toList();

        List<ThresholdViolation> violations = thresholdViolationService.findAll(null, null, departmentId);
        return buildViolationSummary(violations, roomIds, rooms);
    }

    @PreAuthorize("isAuthenticated()")
    public CompanyDashboardDTO getCompanyDashboard() {
        List<Department> departments = departmentService.getAll();

        int totalEmployees = employeeProfileService.getAll(null, null).size();

        int totalRooms = 0;
        int activeViolations = 0;
        List<DepartmentDashboardDTO> departmentDTOs = new ArrayList<>();

        for (Department dept : departments) {
            List<Room> rooms = dept.getRooms().stream()
                    .filter(Room::isActive)
                    .toList();

            totalRooms += rooms.size();

            int deptActiveViolations = rooms.stream()
                    .mapToInt(r -> thresholdViolationService
                            .findAll(ViolationStatus.ACTIVE, r.getId(), null)
                            .size())
                    .sum();

            activeViolations += deptActiveViolations;

            Map<Metric, Double> avgMetrics = computeAvgMetricsForRooms(rooms);

            departmentDTOs.add(new DepartmentDashboardDTO(
                    dept.getId(),
                    dept.getName(),
                    deptActiveViolations,
                    avgMetrics
            ));
        }

        log.debug("Generated company dashboard: departments={}, totalRooms={}, activeViolations={}",
                departments.size(), totalRooms, activeViolations);

        return new CompanyDashboardDTO(
                LocalDateTime.now(),
                totalRooms,
                totalEmployees,
                activeViolations,
                departmentDTOs
        );
    }

    @PreAuthorize("isAuthenticated()")
    public ViolationSummaryDTO getCompanyViolationSummary() {
        List<ThresholdViolation> violations = thresholdViolationService.findAll(null, null, null);

        List<Room> allRooms = departmentService.getAll().stream()
                .flatMap(d -> d.getRooms().stream())
                .filter(Room::isActive)
                .toList();
        List<Long> roomIds = allRooms.stream().map(Room::getId).toList();

        log.debug("Generated company violation summary: totalViolations={}", violations.size());
        return buildViolationSummary(violations, roomIds, allRooms);
    }

    @PreAuthorize("isAuthenticated()")
    public CompanyTrendDTO getCompanyTrend(Metric metric, LocalDateTime from, LocalDateTime to) {
        LocalDateTime effectiveTo   = to   != null ? to   : LocalDateTime.now();
        LocalDateTime effectiveFrom = from != null ? from : effectiveTo.minusDays(7);

        List<Room> allRooms = departmentService.getAll().stream()
                .flatMap(d -> d.getRooms().stream())
                .filter(Room::isActive)
                .toList();

        List<Measurement> measurements = allRooms.stream()
                .flatMap(r -> measurementService
                        .getFiltered(r.getId(), metric, effectiveFrom, effectiveTo).stream())
                .toList();

        String bucketSize = resolveBucketSize(effectiveFrom, effectiveTo);
        List<TrendPointDTO> points = bucketize(measurements, effectiveFrom, effectiveTo, bucketSize);

        log.debug("Generated company trend: metric={}, from={}, to={}, bucketSize={}, points={}",
                metric, effectiveFrom, effectiveTo, bucketSize, points.size());
        return new CompanyTrendDTO(metric, bucketSize, points);
    }

    private MetricSummaryDTO buildMetricSummary(List<Measurement> all, Metric metric) {
        List<Float> values = all.stream()
                .filter(m -> m.getMetric() == metric)
                .sorted(Comparator.comparing(Measurement::getTimestamp))
                .map(Measurement::getMeasurement)
                .toList();

        if (values.isEmpty()) {
            return new MetricSummaryDTO(null, null, null, null, 0L);
        }

        double avg    = values.stream().mapToDouble(Float::doubleValue).average().orElse(0);
        double min    = values.stream().mapToDouble(Float::doubleValue).min().orElse(0);
        double max    = values.stream().mapToDouble(Float::doubleValue).max().orElse(0);
        double latest = values.getLast();

        return new MetricSummaryDTO(latest, avg, min, max, (long) values.size());
    }

    /**
     * Picks a bucket size based on the length of the queried window:
     *
     *  ≤ 2 h    →  5 min
     *  ≤ 48 h   →  1 h
     *  ≤ 14 d   →  6 h
     *  ≤ 90 d   →  1 day
     *  ≤ 365 d  →  1 week
     *  > 365 d  →  1 month
     */
    private String resolveBucketSize(LocalDateTime from, LocalDateTime to) {
        long hours = ChronoUnit.HOURS.between(from, to);
        if (hours <= 2)    return "5m";
        if (hours <= 48)   return "1h";
        if (hours <= 336)  return "6h";
        if (hours <= 2160) return "1d";
        if (hours <= 8760) return "1w";
        return "1M";
    }

    /**
     * Groups measurements into fixed-width time buckets and returns the exact average
     * per bucket. Only populated buckets are returned (sparse).
     * Measurements outside [from, to] are skipped defensively.
     */
    private List<TrendPointDTO> bucketize(List<Measurement> measurements,
                                          LocalDateTime from,
                                          LocalDateTime to,
                                          String bucketSize) {
        if (measurements.isEmpty()) {
            return Collections.emptyList();
        }

        if ("1M".equals(bucketSize)) {
            return bucketizeMonthly(measurements);
        }

        long bucketMinutes = switch (bucketSize) {
            case "5m" -> 5L;
            case "1h" -> 60L;
            case "6h" -> 360L;
            case "1d" -> 1440L;
            case "1w" -> 10080L;
            default   -> 1440L;
        };

        Map<LocalDateTime, List<Float>> buckets = new TreeMap<>();

        for (Measurement m : measurements) {
            LocalDateTime ts = m.getTimestamp();

            if (ts.isBefore(from) || ts.isAfter(to)) {
                continue;
            }

            long minutesSinceFrom = ChronoUnit.MINUTES.between(from, ts);
            long bucketIndex = minutesSinceFrom / bucketMinutes;
            LocalDateTime bucketStart = from.plusMinutes(bucketIndex * bucketMinutes);

            buckets.computeIfAbsent(bucketStart, k -> new ArrayList<>())
                    .add(m.getMeasurement());
        }

        return buckets.entrySet().stream()
                .map(e -> new TrendPointDTO(
                        e.getKey(),
                        e.getValue().stream()
                                .mapToDouble(Float::doubleValue)
                                .average()
                                .orElse(0)))
                .toList();
    }

    /** Monthly bucketing: truncate each timestamp to the 1st of its month, then average. */
    private List<TrendPointDTO> bucketizeMonthly(List<Measurement> measurements) {
        Map<LocalDateTime, List<Float>> buckets = new TreeMap<>();

        for (Measurement m : measurements) {
            LocalDateTime monthStart = m.getTimestamp()
                    .withDayOfMonth(1)
                    .truncatedTo(ChronoUnit.DAYS);
            buckets.computeIfAbsent(monthStart, k -> new ArrayList<>())
                    .add(m.getMeasurement());
        }

        return buckets.entrySet().stream()
                .map(e -> new TrendPointDTO(
                        e.getKey(),
                        e.getValue().stream()
                                .mapToDouble(Float::doubleValue)
                                .average()
                                .orElse(0)))
                .toList();
    }

    private Map<Metric, Double> computeAvgMetricsForRooms(List<Room> rooms) {
        if (rooms.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Measurement> allMeasurements = rooms.stream()
                .flatMap(r -> measurementService.getFiltered(r.getId(), null, null, null).stream())
                .toList();

        return Arrays.stream(Metric.values())
                .collect(Collectors.toMap(
                        metric -> metric,
                        metric -> allMeasurements.stream()
                                .filter(m -> m.getMetric() == metric)
                                .mapToDouble(Measurement::getMeasurement)
                                .average()
                                .orElse(0.0)
                ));
    }

    private ViolationSummaryDTO buildViolationSummary(List<ThresholdViolation> violations,
                                                      List<Long> roomIds,
                                                      List<Room> rooms) {
        int total    = violations.size();
        int active   = (int) violations.stream()
                .filter(v -> v.getViolationStatus() == ViolationStatus.ACTIVE).count();
        int resolved = (int) violations.stream()
                .filter(v -> v.getViolationStatus() == ViolationStatus.RESOLVED).count();

        List<ViolationBreakdownDTO> byMetric = Arrays.stream(Metric.values())
                .map(metric -> new ViolationBreakdownDTO(
                        metric.name(),
                        (int) violations.stream().filter(v -> v.getMetric() == metric).count()))
                .filter(dto -> dto.count() > 0)
                .toList();

        Map<Long, String> roomNameById = rooms.stream()
                .collect(Collectors.toMap(Room::getId, Room::getName));

        List<ViolationBreakdownDTO> byRoom = roomIds.stream()
                .map(rid -> new ViolationBreakdownDTO(
                        roomNameById.getOrDefault(rid, "Room " + rid),
                        (int) violations.stream()
                                .filter(v -> v.getRoom() != null && rid.equals(v.getRoom().getId()))
                                .count()))
                .filter(dto -> dto.count() > 0)
                .toList();

        return new ViolationSummaryDTO(total, active, resolved, byMetric, byRoom);
    }
}
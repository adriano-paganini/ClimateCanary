import "../styles/App.css";
import "../styles/AbsenceCalendar.css";
import "primereact/resources/themes/lara-light-cyan/theme.css";
import "primeicons/primeicons.css";
import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { addDays, addYears, format } from "date-fns";
import type { Plugin } from "chart.js";
import { Button } from "primereact/button";
import { Card } from "primereact/card";
import { Chart } from "primereact/chart";
import { Column } from "primereact/column";
import { ConfirmDialog, confirmDialog } from "primereact/confirmdialog";
import { DataTable } from "primereact/datatable";
import { Dialog } from "primereact/dialog";
import { Dropdown } from "primereact/dropdown";
import { InputNumber } from "primereact/inputnumber";
import { InputSwitch } from "primereact/inputswitch";
import { InputText } from "primereact/inputtext";
import { Message } from "primereact/message";
import { ProgressSpinner } from "primereact/progressspinner";
import { Tag } from "primereact/tag";
import { Toast } from "primereact/toast";
import RoomClimateReportPanel from "../utilities/ReportClimateReportPanel";
import { useUser } from "../Contexts/AuthenticatedUserContext";
import { ReportService } from "../services/ReportService";


import NavbarComponent from "../components/NavbarComponent";
import {
    BuildingDTO,
    DepartmentDTO,
    GetAllViolationStatusEnum,
    MeasurementDTOMetricEnum,
    RoomDTO,
    RoomType,
    ThresholdCreateDTO,
    ThresholdCreateDTOMetricEnum,
    ThresholdCreateDTOThresholdTypeEnum,
    ThresholdDTO,
    ThresholdDTOThresholdTypeEnum,
    ThresholdUpdateDTO,
    ThresholdViolationDTO,
    ThresholdViolationDTOViolationStatusEnum,
} from "../generated-skeleton-api";
import { AnalyticsService, RoomSummaryDTO, RoomTrendDTO, RoomViolationSummaryDTO } from "../services/AnalyticsService";
import { BuildingService } from "../services/BuildingService";
import { DepartmentService } from "../services/DepartmentService";
import { RoomService } from "../services/RoomService";
import { ThresholdService } from "../services/ThresholdService";
import { ViolationService } from "../services/ViolationService";
import NoDataOverlay from "../components/NoDataOverlay";
import ViolationList from "../components/ViolationList";
import { findGapRanges } from "../utilities/dataGapUtils";

type TimeRange = "24h" | "7d" | "30d" | "90d" | "1y";

const TIME_RANGES: { label: string; value: TimeRange; bucketHint: string }[] = [
    { label: "24 hours", value: "24h", bucketHint: "raw" },
    { label: "7 days", value: "7d", bucketHint: "raw" },
    { label: "30 days", value: "30d", bucketHint: "1h" },
    { label: "90 days", value: "90d", bucketHint: "1h" },
    { label: "1 year", value: "1y", bucketHint: "6h" },
];

const METRICS: { key: MeasurementDTOMetricEnum; label: string; unit: string; color: string }[] = [
    { key: MeasurementDTOMetricEnum.TEMPERATURE, label: "Temperature", unit: "C", color: "#f97316" },
    { key: MeasurementDTOMetricEnum.HUMIDITY, label: "Humidity", unit: "%", color: "#2563eb" },
    { key: MeasurementDTOMetricEnum.PRESSURE, label: "Pressure", unit: "hPa", color: "#7c3aed" },
    { key: MeasurementDTOMetricEnum.IAQ, label: "Air Quality", unit: "", color: "#16a34a" },
];

const CHART_OPTIONS = {
    responsive: true,
    maintainAspectRatio: false,
    animation: false as const,
    plugins: {
        legend: { position: "top" as const, labels: { boxWidth: 12, font: { size: 11 } } },
    },
    scales: {
        x: { ticks: { maxTicksLimit: 10, maxRotation: 0, font: { size: 10 } } },
        y: { ticks: { font: { size: 10 } } },
    },
};

const toLocalDateTimeParam = (date: Date): string => format(date, "yyyy-MM-dd'T'HH:mm:ss");

const dateOnly = (date: Date): string => format(date, "yyyy-MM-dd");

const dateFromInput = (value: string): Date | null => {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return null;
    const date = new Date(`${value}T00:00:00`);
    return Number.isNaN(date.getTime()) ? null : date;
};

const rangeStart = (date: Date): Date =>
    new Date(date.getFullYear(), date.getMonth(), date.getDate());

const isFutureRangeAnchor = (date: Date): boolean =>
    rangeStart(date).getTime() > rangeStart(new Date()).getTime();

const clampToToday = (date: Date): Date =>
    isFutureRangeAnchor(date) ? rangeStart(new Date()) : date;

const shiftRangeAnchor = (date: Date, range: TimeRange, direction: -1 | 1): Date => {
    switch (range) {
        case "24h":
            return addDays(date, direction);
        case "7d":
            return addDays(date, 7 * direction);
        case "30d":
            return addDays(date, 30 * direction);
        case "90d":
            return addDays(date, 90 * direction);
        case "1y":
            return addYears(date, direction);
    }
};

const rangeWindow = (date: Date, range: TimeRange): { from: Date; to: Date } => {
    const endDay = rangeStart(date);
    const to = addDays(endDay, 1);
    switch (range) {
        case "24h":
            return { from: endDay, to };
        case "7d":
            return { from: addDays(endDay, -6), to };
        case "30d":
            return { from: addDays(endDay, -29), to };
        case "90d":
            return { from: addDays(endDay, -89), to };
        case "1y":
            return { from: addDays(addYears(endDay, -1), 1), to };
    }
};

const visibleRangeLabel = (date: Date, range: TimeRange): string => {
    const window = rangeWindow(date, range);
    return `${format(window.from, "dd.MM.yy")} - ${format(addDays(window.to, -1), "dd.MM.yy")}`;
};

const tickLabel = (timestamp: string, range: TimeRange): string => {
    const date = new Date(timestamp);
    if (range === "24h") return format(date, "HH:mm");
    if (range === "7d" || range === "30d") return format(date, "MM/dd HH:mm");
    return format(date, "MM/dd");
};

const thresholdOnlyLabels = (range: TimeRange): string[] => {
    if (range === "24h") return ["00:00", "06:00", "12:00", "18:00", "24:00"];
    if (range === "7d") return ["Day 1", "Day 2", "Day 3", "Day 4", "Day 5", "Day 6", "Day 7"];
    if (range === "30d") return ["Week 1", "Week 2", "Week 3", "Week 4", "Week 5"];
    if (range === "90d") return ["Month 1", "Month 2", "Month 3"];
    return ["Q1", "Q2", "Q3", "Q4"];
};

const formatNumber = (value?: number, digits = 1): string =>
    value === undefined || value === null ? "-" : value.toFixed(digits);

const metricLabel = (metric?: string): string =>
    METRICS.find(m => m.key === metric)?.label ?? metric?.replace("_", " ") ?? "-";

const RoomManagementView: React.FC = () => {
    const { currentUser } = useUser();
    const [rooms, setRooms] = useState<RoomDTO[]>([]);
    const [departments, setDepartments] = useState<DepartmentDTO[]>([]);
    const [buildings, setBuildings] = useState<BuildingDTO[]>([]);
    const [selectedRoomId, setSelectedRoomId] = useState<number | null>(null);
    const [roomFilter, setRoomFilter] = useState("");
    const [timeRange, setTimeRange] = useState<TimeRange>("24h");
    const [currentDate, setCurrentDate] = useState(new Date());
    const [selectedMetric, setSelectedMetric] = useState<MeasurementDTOMetricEnum>(MeasurementDTOMetricEnum.TEMPERATURE);

    const [summary, setSummary] = useState<RoomSummaryDTO | null>(null);
    const [trend, setTrend] = useState<RoomTrendDTO | null>(null);
    const [violationSummary, setViolationSummary] = useState<RoomViolationSummaryDTO | null>(null);
    const [violations, setViolations] = useState<ThresholdViolationDTO[]>([]);
    const [thresholds, setThresholds] = useState<ThresholdDTO[]>([]);
    const [loadingRooms, setLoadingRooms] = useState(true);
    const [loadingAnalytics, setLoadingAnalytics] = useState(false);
    const [analyticsError, setAnalyticsError] = useState<string | null>(null);
    const [privacyRestricted, setPrivacyRestricted] = useState(false);

    const toast = useRef<Toast | null>(null);
    const analyticsRequestId = useRef(0);

    // Threshold edit dialog state
    const [thresholdDialogVisible, setThresholdDialogVisible] = useState(false);
    const [editingThreshold, setEditingThreshold] = useState<ThresholdDTO | null>(null);
    const [thresholdForm, setThresholdForm] = useState<Partial<ThresholdCreateDTO> & { enabled: boolean }>({
        roomId: undefined, metric: undefined, boundValue: undefined, thresholdType: undefined, climateHintIds: [], enabled: true,
    });
    const [boundValueError, setBoundValueError] = useState<string | null>(null);

    const selectedRoom = useMemo(
        () => rooms.find(room => room.id === selectedRoomId) ?? null,
        [rooms, selectedRoomId],
    );

    const getDepartmentName = useCallback((id?: number) => {
        if (!id) return "-";
        return departments.find(d => d.id === id)?.name ?? `Department ${id}`;
    }, [departments]);

    const getBuildingName = useCallback((id?: number) => {
        if (!id) return "-";
        return buildings.find(b => b.id === id)?.name ?? `Building ${id}`;
    }, [buildings]);

    useEffect(() => {
        const loadReferenceData = async () => {
            const [buildingData, departmentData] = await Promise.all([
                BuildingService.getAll().catch(() => []),
                DepartmentService.getAll().catch(() => []),
            ]);
            setBuildings(buildingData);
            setDepartments(departmentData);
        };

        const loadRooms = async () => {
            setLoadingRooms(true);
            try {
                const data = await RoomService.getAll();
                const activeRooms = data.filter(room => room.active !== false);
                setRooms(activeRooms);
                setSelectedRoomId(current => current ?? activeRooms[0]?.id ?? null);
            } catch {
                toast.current?.show({
                    severity: "error",
                    summary: "Error",
                    detail: "Failed to load rooms",
                    life: 3000,
                });
            } finally {
                setLoadingRooms(false);
            }
        };

        void loadReferenceData();
        void loadRooms();
    }, []);

    useEffect(() => {
        if (!selectedRoomId) return;

        const requestId = analyticsRequestId.current + 1;
        analyticsRequestId.current = requestId;

        const loadAnalytics = async () => {
            setLoadingAnalytics(true);
            setAnalyticsError(null);
            setPrivacyRestricted(false);
            setSummary(null);
            setTrend(null);
            setViolationSummary(null);
            setViolations([]);
            setThresholds([]);
            const window = rangeWindow(currentDate, timeRange);
            const from = toLocalDateTimeParam(window.from);
            const to = toLocalDateTimeParam(window.to);
            const isCurrentRequest = () => analyticsRequestId.current === requestId;

            try {
                const summaryData = await AnalyticsService.getRoomSummary(selectedRoomId, from, to);
                if (!isCurrentRequest()) return;
                setSummary(summaryData);

                const violationSummaryData = await AnalyticsService.getRoomViolations(selectedRoomId);
                if (!isCurrentRequest()) return;
                setViolationSummary(violationSummaryData);

                const [activeViolations, resolvedViolations] = await Promise.all([
                    ViolationService.getAll({
                        roomId: selectedRoomId,
                        violationStatus: GetAllViolationStatusEnum.ACTIVE,
                    }),
                    ViolationService.getAll({
                        roomId: selectedRoomId,
                        violationStatus: GetAllViolationStatusEnum.RESOLVED,
                    }),
                ]);
                if (!isCurrentRequest()) return;
                setViolations([...activeViolations, ...resolvedViolations]);

                const thresholdData = await ThresholdService.getAll({ roomId: selectedRoomId });
                if (!isCurrentRequest()) return;
                setThresholds(thresholdData);

                const trendData = await AnalyticsService.getRoomTrend(selectedRoomId, selectedMetric, from, to);
                if (!isCurrentRequest()) return;
                setTrend(trendData);
            } catch (err: unknown) {
                if (!isCurrentRequest()) return;
                const status = (err as { response?: { status?: number } })?.response?.status;
                if (status === 403) {
                    setPrivacyRestricted(true);
                } else {
                    setAnalyticsError("Failed to load room climate data.");
                }
                setSummary(null);
                setTrend(null);
                setViolationSummary(null);
                setViolations([]);
                setThresholds([]);
            } finally {
                if (isCurrentRequest()) {
                    setLoadingAnalytics(false);
                }
            }
        };

        void loadAnalytics();
    }, [selectedRoomId, selectedMetric, currentDate, timeRange]);

    const filteredRooms = useMemo(() => {
        const query = roomFilter.trim().toLowerCase();
        if (!query) return rooms;

        return rooms.filter(room => [
            room.name,
            room.roomType,
            getDepartmentName(room.departmentId),
            getBuildingName(room.buildingId),
        ].some(value => value?.toLowerCase().includes(query)));
    }, [getBuildingName, getDepartmentName, roomFilter, rooms]);

    const buildChartData = () => {
        const metric = METRICS.find(m => m.key === selectedMetric) ?? METRICS[0];
        const points = [...(trend?.points ?? [])]
            .filter(point => point.timestamp && point.value !== undefined && point.value !== null)
            .sort((a, b) => new Date(a.timestamp!).getTime() - new Date(b.timestamp!).getTime());

        const activeThresholds = thresholds.filter(threshold =>
            threshold.metric === selectedMetric &&
            threshold.enabled !== false &&
            threshold.boundValue !== undefined
        );
        const labels = points.length > 0
            ? points.map(point => tickLabel(point.timestamp!, timeRange))
            : activeThresholds.length > 0
                ? thresholdOnlyLabels(timeRange)
                : [];
        const datasets = [{
            label: metric.label,
            data: points.length > 0 ? points.map(point => point.value ?? null) : labels.map(() => null),
            fill: false,
            borderColor: metric.color,
            backgroundColor: `${metric.color}33`,
            tension: 0.25,
            pointRadius: points.length > 160 ? 0 : 3,
            spanGaps: false,
        }];

        activeThresholds
            .forEach(threshold => {
                const isUpper = threshold.thresholdType === ThresholdDTOThresholdTypeEnum.UPPER;
                datasets.push({
                    label: isUpper ? "Upper threshold" : "Lower threshold",
                    data: Array(labels.length).fill(threshold.boundValue),
                    fill: false,
                    borderColor: isUpper ? "#dc2626" : "#d97706",
                    backgroundColor: "transparent",
                    tension: 0,
                    pointRadius: 0,
                    spanGaps: false,
                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                } as any);
            });

        return { labels, datasets };
    };

    const jumpToDate = (value: string) => {
        const nextDate = dateFromInput(value);
        if (!nextDate) return;
        setCurrentDate(clampToToday(nextDate));
    };

    const navigateRange = (direction: -1 | 1) => {
        const nextDate = shiftRangeAnchor(rangeStart(currentDate), timeRange, direction);
        setCurrentDate(clampToToday(nextDate));
    };

    const THRESHOLD_METRIC_BOUNDS: Record<string, { min: number; max: number; unit: string }> = {
        TEMPERATURE: { min: -50, max: 100, unit: "°C" },
        HUMIDITY: { min: 0, max: 100, unit: "%" },
        PRESSURE: { min: 870, max: 1085, unit: "hPa" },
        IAQ: { min: 0, max: 500, unit: "" },
    };

    const openEditThreshold = (threshold: ThresholdDTO) => {
        setThresholdForm({
            roomId: threshold.roomId,
            metric: threshold.metric as ThresholdCreateDTOMetricEnum | undefined,
            boundValue: threshold.boundValue,
            thresholdType: threshold.thresholdType as ThresholdCreateDTOThresholdTypeEnum | undefined,
            climateHintIds: threshold.climateHintIds ?? [],
            enabled: threshold.enabled ?? true,
        });
        setEditingThreshold(threshold);
        setBoundValueError(null);
        setThresholdDialogVisible(true);
    };

    const saveThreshold = async () => {
        setBoundValueError(null);
        if (!editingThreshold?.id || !thresholdForm.metric || thresholdForm.boundValue === undefined || !thresholdForm.thresholdType) {
            toast.current?.show({ severity: "warn", summary: "Validation", detail: "Please fill in all required fields", life: 3000 });
            return;
        }
        const bounds = THRESHOLD_METRIC_BOUNDS[thresholdForm.metric];
        if (bounds && (thresholdForm.boundValue < bounds.min || thresholdForm.boundValue > bounds.max)) {
            setBoundValueError(`Value must be between ${bounds.min} and ${bounds.max}${bounds.unit ? " " + bounds.unit : ""}.`);
            return;
        }
        try {
            const dto: ThresholdUpdateDTO = {
                roomId: thresholdForm.roomId,
                metric: thresholdForm.metric,
                boundValue: thresholdForm.boundValue,
                thresholdType: thresholdForm.thresholdType,
                climateHintIds: thresholdForm.climateHintIds,
                enabled: thresholdForm.enabled,
            };
            const updated = await ThresholdService.update(editingThreshold.id, dto);
            setThresholds(prev => prev.map(t => t.id === updated.id ? updated : t));
            toast.current?.show({ severity: "success", summary: "Updated", detail: "Threshold updated", life: 3000 });
            setThresholdDialogVisible(false);
        } catch {
            toast.current?.show({ severity: "error", summary: "Error", detail: "Failed to save threshold", life: 3000 });
        }
    };

    const confirmDeleteThreshold = (threshold: ThresholdDTO) => {
        confirmDialog({
            message: `Delete the ${threshold.metric} ${threshold.thresholdType?.toLowerCase()} threshold (${threshold.boundValue}) for this room?`,
            header: "Confirm Delete",
            icon: "pi pi-exclamation-triangle",
            acceptClassName: "p-button-danger",
            accept: () => deleteThreshold(threshold),
        });
    };

    const deleteThreshold = async (threshold: ThresholdDTO) => {
        if (!threshold.id) return;
        try {
            await ThresholdService.delete(threshold.id);
            setThresholds(prev => prev.filter(t => t.id !== threshold.id));
            toast.current?.show({ severity: "success", summary: "Deleted", detail: "Threshold deleted", life: 3000 });
        } catch {
            toast.current?.show({ severity: "error", summary: "Error", detail: "Failed to delete threshold", life: 3000 });
        }
    };

    const toggleThresholdEnabled = async (threshold: ThresholdDTO) => {
        if (!threshold.id) return;
        try {
            const updated = await ThresholdService.update(threshold.id, { enabled: !threshold.enabled });
            setThresholds(prev => prev.map(t => t.id === updated.id ? updated : t));
        } catch {
            toast.current?.show({ severity: "error", summary: "Error", detail: "Failed to update threshold", life: 3000 });
        }
    };

    const handleDisableViolation = async (violation: ThresholdViolationDTO) => {
        if (!violation.id) return;
        try {
            const updated = await ViolationService.disable(violation.id);
            setViolations(prev => prev.map(v => v.id === updated.id ? updated : v));
            toast.current?.show({ severity: "success", summary: "Deactivated", detail: "Threshold violation deactivated", life: 3000 });
        } catch {
            toast.current?.show({ severity: "error", summary: "Error", detail: "Failed to deactivate violation", life: 3000 });
        }
    };

    const buildGapPlugin = (): Plugin => {
        const points = (trend?.points ?? [])
            .filter(p => p.timestamp)
            .map(p => ({ timestamp: p.timestamp! }));
        const gaps = findGapRanges(points);
        return {
            id: "gapHighlight",
            beforeDraw(chart) {
                if (gaps.length === 0) return;
                const ctx = chart.ctx;
                const xScale = chart.scales["x"];
                const yScale = chart.scales["y"];
                ctx.save();
                ctx.fillStyle = "rgba(156, 163, 175, 0.25)";
                for (const { startIdx, endIdx } of gaps) {
                    const x1 = xScale.getPixelForValue(startIdx);
                    const x2 = xScale.getPixelForValue(endIdx);
                    ctx.fillRect(x1, yScale.top, x2 - x1, yScale.bottom - yScale.top);
                }
                ctx.restore();
            },
        };
    };

    const summaryForMetric = (metric: MeasurementDTOMetricEnum) => summary?.metrics?.[metric];

    const roomTypeTemplate = (row: RoomDTO) => (
        <Tag
            value={row.roomType === RoomType.COMMON_AREAS ? "Common" : "Office"}
            severity={row.roomType === RoomType.OFFICE ? "info" : "warning"}
        />
    );

    const violationStatusTemplate = (row: ThresholdViolationDTO) => (
        <Tag
            value={row.violationStatus ?? "-"}
            severity={row.violationStatus === ThresholdViolationDTOViolationStatusEnum.ACTIVE ? "danger" : "success"}
        />
    );

    const dateTemplate = (value?: string) => value ? format(new Date(value), "yyyy-MM-dd HH:mm") : "-";

    const chartData = buildChartData();
    const chartEmpty = (chartData.datasets[0].data as (number | null)[]).every(value => value === null);
    const chartHasThresholds = chartData.datasets.length > 1;
    const nextRangeWouldBeFuture = isFutureRangeAnchor(
        shiftRangeAnchor(rangeStart(currentDate), timeRange, 1)
    );

    return (
        <div>
            <NavbarComponent />
            <Toast ref={toast} />

            <div className="room-management-page">
                <div className="room-management-header">
                    <div>
                        <h2 style={{ margin: 0 }}>Room Climate Data</h2>
                        <p style={{ margin: "0.35rem 0 0", color: "#64748b" }}>
                            Building administration view for persisted measurements, trends, and threshold violations.
                        </p>
                    </div>
                </div>

                <div className="room-management-layout">
                    <Card className="room-management-room-card">
                        <div style={{ display: "flex", flexDirection: "column", gap: "0.75rem" }}>
                            <InputText
                                value={roomFilter}
                                onChange={event => setRoomFilter(event.target.value)}
                                placeholder="Search rooms..."
                                style={{ width: "100%" }}
                            />

                            <DataTable
                                value={filteredRooms}
                                loading={loadingRooms}
                                emptyMessage="No rooms found."
                                selectionMode="single"
                                selection={selectedRoom}
                                onSelectionChange={event => setSelectedRoomId((event.value as RoomDTO | null)?.id ?? null)}
                                dataKey="id"
                                stripedRows
                                paginator
                                rows={10}
                                sortField="name"
                                sortOrder={1}
                                className="room-management-table"
                                tableStyle={{ tableLayout: "fixed", width: "100%" }}
                            >
                                <Column field="name" header="Room" sortable style={{ width: "34%" }} />
                                <Column field="roomType" header="Type" body={roomTypeTemplate} sortable style={{ width: "28%" }} className="room-management-optional-column" />
                                <Column
                                    header="Department"
                                    body={(row: RoomDTO) => getDepartmentName(row.departmentId)}
                                    sortable
                                    sortField="departmentId"
                                    sortFunction={(event) =>
                                        [...event.data].sort((a, b) =>
                                            (event.order ?? 1) *
                                            getDepartmentName(a.departmentId).localeCompare(getDepartmentName(b.departmentId))
                                        )
                                    }
                                    className="room-management-optional-column"
                                    style={{ width: "38%" }}
                                />
                            </DataTable>
                        </div>
                    </Card>
                    <div className="room-management-detail-stack">
                        {!selectedRoomId ? (
                            <Message severity="info" text="Select a room to view climate data." />
                        ) : (
                            <>
                                <Card>
                                    <div className="room-management-card-header">
                                        <div>
                                            <h3 style={{ margin: 0 }}>{selectedRoom?.name ?? `Room ${selectedRoomId}`}</h3>
                                            <p style={{ margin: "0.35rem 0 0", color: "#64748b" }}>
                                                {getBuildingName(selectedRoom?.buildingId)} - {getDepartmentName(selectedRoom?.departmentId)}
                                            </p>
                                        </div>

                                        <div
                                            className="room-management-actions"
                                            style={{
                                                display: "flex",
                                                alignItems: "center",
                                                gap: "0.5rem"
                                            }}
                                        >
                                            <Dropdown
                                                value={selectedMetric}
                                                options={METRICS.map(metric => ({
                                                    label: metric.label,
                                                    value: metric.key
                                                }))}
                                                onChange={event => setSelectedMetric(event.value)}
                                                className="room-management-metric-dropdown"
                                                style={{ height: "2.5rem" }}
                                            />

                                            <div
                                                style={{
                                                    display: "flex",
                                                    alignItems: "center",
                                                    height: "2.5rem"
                                                }}
                                            >
                                                <RoomClimateReportPanel
                                                    rooms={rooms}
                                                    currentUser={currentUser}
                                                    onSendReport={ReportService.sendReportEmail}
                                                />
                                            </div>
                                        </div>

                                    </div>

                                    <div className="absence-calendar-header">
                                        <div className="absence-agenda-control">
                                            <select
                                                id="room-climate-timeframe"
                                                value={timeRange}
                                                onChange={event => setTimeRange(event.target.value as TimeRange)}
                                            >
                                                {TIME_RANGES.map(range => (
                                                    <option key={range.value} value={range.value}>
                                                        {range.label} ({range.bucketHint})
                                                    </option>
                                                ))}
                                            </select>
                                        </div>

                                        <div className="absence-calendar-navigation">
                                            <button type="button" onClick={() => navigateRange(-1)} aria-label="Previous range">
                                                <i className="pi pi-chevron-left" aria-hidden="true" />
                                            </button>
                                            <button type="button" onClick={() => setCurrentDate(new Date())}>
                                                Today
                                            </button>
                                            <button
                                                type="button"
                                                onClick={() => navigateRange(1)}
                                                aria-label="Next range"
                                                disabled={nextRangeWouldBeFuture}
                                            >
                                                <i className="pi pi-chevron-right" aria-hidden="true" />
                                            </button>
                                        </div>

                                        <div className="absence-calendar-range">
                                            {visibleRangeLabel(currentDate, timeRange)}
                                        </div>

                                        <div className="absence-date-jump">
                                            <label htmlFor="room-climate-jump-date">Jump to</label>
                                            <input
                                                id="room-climate-jump-date"
                                                type="date"
                                                max={dateOnly(new Date())}
                                                value={dateOnly(currentDate)}
                                                onChange={event => jumpToDate(event.target.value)}
                                            />
                                        </div>
                                    </div>

                                    {selectedRoom?.privacyMode && (
                                        <div style={{
                                            display: "flex", alignItems: "center", gap: "0.6rem",
                                            padding: "0.65rem 1rem", marginBottom: "1rem",
                                            backgroundColor: "#f3f4f6", border: "1px solid #d1d5db",
                                            borderRadius: "6px", color: "#374151", fontSize: "0.9rem",
                                        }}>
                                            <i className="pi pi-lock" style={{ color: "#6b7280" }} />
                                            <span>Datenschutz aktiv — Messdaten werden nur bei ausreichender Raumbelegung (mind. 5 Personen) erfasst. Graue Bereiche zeigen Perioden ohne Daten.</span>
                                        </div>
                                    )}

                                    {analyticsError && <Message severity="error" text={analyticsError} style={{ marginBottom: "1rem" }} />}
                                    {privacyRestricted && (
                                        <Message severity="warn" text="Klimadaten nicht verfügbar — Datenschutz aktiv (Belegung unter Mindestanzahl)." style={{ marginBottom: "1rem" }} />
                                    )}

                                    {loadingAnalytics ? (
                                        <div style={{ minHeight: "360px", display: "flex", alignItems: "center", justifyContent: "center" }}>
                                            <ProgressSpinner />
                                        </div>
                                    ) : privacyRestricted || (chartEmpty && !chartHasThresholds) ? (
                                        <NoDataOverlay
                                            height="360px"
                                            message={privacyRestricted ? "Datenschutz aktiv — keine Daten verfügbar" : "No trend data for the selected room and period."}
                                            icon={privacyRestricted ? "pi pi-lock" : "pi pi-ban"}
                                        />
                                    ) : (
                                        <>
                                            <div style={{ display: "flex", alignItems: "center", gap: "0.5rem", marginBottom: "0.75rem" }}>
                                                <Tag value={`Bucket: ${trend?.bucketSize ?? "-"}`} severity="info" />
                                                {trend?.granularityReduced && <Tag value="Reduced granularity" severity="warning" />}
                                            </div>
                                            <div className="room-management-chart">
                                                <Chart type="line" data={chartData} options={CHART_OPTIONS} plugins={[buildGapPlugin()]} style={{ height: "100%" }} />
                                            </div>
                                        </>
                                    )}
                                </Card>

                                <div className="room-management-summary-grid">
                                    {METRICS.map(metric => {
                                        const stats = summaryForMetric(metric.key);
                                        return (
                                            <Card key={metric.key}>
                                                <div style={{ color: "#64748b", fontSize: "0.85rem", marginBottom: "0.35rem" }}>{metric.label}</div>
                                                <div style={{ fontSize: "1.4rem", fontWeight: 700 }}>
                                                    {formatNumber(stats?.latest)}{metric.unit && ` ${metric.unit}`}
                                                </div>
                                                <div style={{ color: "#64748b", fontSize: "0.8rem", marginTop: "0.4rem" }}>
                                                    Avg {formatNumber(stats?.avg)} - Min {formatNumber(stats?.min)} - Max {formatNumber(stats?.max)}
                                                </div>
                                            </Card>
                                        );
                                    })}
                                </div>

                                <Card>
                                    <div style={{ display: "flex", justifyContent: "space-between", gap: "1rem", alignItems: "center", marginBottom: "1rem", flexWrap: "wrap" }}>
                                        <div>
                                            <h3 style={{ margin: 0 }}>Threshold Violations</h3>
                                            <p style={{ margin: "0.35rem 0 0", color: "#64748b" }}>
                                                Active: {violationSummary?.active ?? 0} - Resolved: {violationSummary?.resolved ?? 0} - Total: {violationSummary?.total ?? 0}
                                            </p>
                                        </div>
                                        <div style={{ display: "flex", gap: "0.5rem", flexWrap: "wrap" }}>
                                            {(violationSummary?.byMetric ?? []).map(item => (
                                                <Tag key={item.label} value={`${metricLabel(item.label)}: ${item.count ?? 0}`} severity="info" />
                                            ))}
                                        </div>
                                    </div>

                                    <ViolationList
                                        violations={violations}
                                        onDisable={handleDisableViolation}
                                    />
                                </Card>

                                {/* Thresholds card */}
                                <Card>
                                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "1rem", flexWrap: "wrap", gap: "0.5rem" }}>
                                        <div>
                                            <h3 style={{ margin: 0 }}>Thresholds</h3>
                                            <p style={{ margin: "0.35rem 0 0", color: "#64748b" }}>
                                                Alert rules configured for this room
                                            </p>
                                        </div>
                                        <Tag
                                            value={`${thresholds.length} threshold${thresholds.length === 1 ? "" : "s"}`}
                                            severity="info"
                                        />
                                    </div>
                                    <DataTable
                                        value={thresholds}
                                        emptyMessage="No thresholds configured for this room."
                                        stripedRows
                                        sortField="metric"
                                        sortOrder={1}
                                    >
                                        <Column
                                            field="metric"
                                            header="Metric"
                                            sortable
                                            body={(row: ThresholdDTO) => (
                                                <Tag value={row.metric ?? "-"} severity={
                                                    row.metric === "TEMPERATURE" ? "danger" :
                                                        row.metric === "HUMIDITY" ? "info" :
                                                            row.metric === "PRESSURE" ? "warning" :
                                                                row.metric === "IAQ" ? "success" : "secondary"
                                                } />
                                            )}
                                            style={{ width: "9rem" }}
                                        />
                                        <Column field="thresholdType" header="Type" sortable style={{ width: "7rem" }} />
                                        <Column
                                            field="boundValue"
                                            header="Limit"
                                            sortable
                                            body={(row: ThresholdDTO) => formatNumber(row.boundValue)}
                                            style={{ width: "6rem" }}
                                        />
                                        <Column
                                            field="enabled"
                                            header="Enabled"
                                            style={{ width: "6rem" }}
                                            body={(row: ThresholdDTO) => (
                                                <InputSwitch
                                                    checked={row.enabled ?? false}
                                                    onChange={() => toggleThresholdEnabled(row)}
                                                />
                                            )}
                                        />
                                        <Column
                                            header="Actions"
                                            style={{ width: "8rem" }}
                                            body={(row: ThresholdDTO) => (
                                                <div style={{ display: "flex", gap: "0.25rem" }}>
                                                    <Button
                                                        icon="pi pi-pencil"
                                                        rounded
                                                        text
                                                        severity="info"
                                                        aria-label="Edit threshold"
                                                        onClick={() => openEditThreshold(row)}
                                                    />
                                                    <Button
                                                        icon="pi pi-trash"
                                                        rounded
                                                        text
                                                        severity="danger"
                                                        aria-label="Delete threshold"
                                                        onClick={() => confirmDeleteThreshold(row)}
                                                    />
                                                </div>
                                            )}
                                        />
                                    </DataTable>
                                </Card>
                            </>
                        )}
                    </div>
                </div>
            </div>

            <ConfirmDialog />

            {/* Edit threshold dialog */}
            <Dialog
                visible={thresholdDialogVisible}
                header="Edit Threshold"
                onHide={() => setThresholdDialogVisible(false)}
                style={{ width: "460px" }}
                footer={
                    <div>
                        <Button label="Cancel" icon="pi pi-times" text onClick={() => setThresholdDialogVisible(false)} />
                        <Button label="Save" icon="pi pi-check" onClick={saveThreshold} />
                    </div>
                }
                modal
                draggable={false}
            >
                <div style={{ display: "flex", flexDirection: "column", gap: "1rem", paddingTop: "0.5rem" }}>
                    <div className="field">
                        <label htmlFor="rm-t-metric">Metric</label>
                        <Dropdown
                            id="rm-t-metric"
                            value={thresholdForm.metric}
                            options={Object.values(ThresholdCreateDTOMetricEnum).map(v => ({ label: v, value: v }))}
                            onChange={e => setThresholdForm(f => ({ ...f, metric: e.value, boundValue: undefined }))}
                            placeholder="Select metric"
                            style={{ width: "100%" }}
                            appendTo="self"
                        />
                    </div>
                    <div className="field">
                        <label htmlFor="rm-t-type">Type</label>
                        <Dropdown
                            id="rm-t-type"
                            value={thresholdForm.thresholdType}
                            options={Object.values(ThresholdCreateDTOThresholdTypeEnum).map(v => ({ label: v, value: v }))}
                            onChange={e => setThresholdForm(f => ({ ...f, thresholdType: e.value }))}
                            placeholder="LOWER / UPPER"
                            style={{ width: "100%" }}
                            appendTo="self"
                        />
                    </div>
                    <div className="field">
                        <label htmlFor="rm-t-value">
                            Limit Value
                            {thresholdForm.metric && THRESHOLD_METRIC_BOUNDS[thresholdForm.metric] && (
                                <span style={{ fontWeight: 400, color: "#6b7280", marginLeft: "0.4rem", fontSize: "0.85rem" }}>
                                    ({THRESHOLD_METRIC_BOUNDS[thresholdForm.metric].min}–{THRESHOLD_METRIC_BOUNDS[thresholdForm.metric].max}{THRESHOLD_METRIC_BOUNDS[thresholdForm.metric].unit ? " " + THRESHOLD_METRIC_BOUNDS[thresholdForm.metric].unit : ""})
                                </span>
                            )}
                        </label>
                        <InputNumber
                            id="rm-t-value"
                            value={thresholdForm.boundValue ?? null}
                            onValueChange={e => { setBoundValueError(null); setThresholdForm(f => ({ ...f, boundValue: e.value ?? undefined })); }}
                            placeholder="Enter limit value"
                            className={boundValueError ? "p-invalid" : undefined}
                            style={{ width: "100%" }}
                            useGrouping={false}
                            maxFractionDigits={2}
                        />
                        {boundValueError && <small className="p-error">{boundValueError}</small>}
                    </div>
                    <div className="field" style={{ display: "flex", alignItems: "center", gap: "0.75rem" }}>
                        <label htmlFor="rm-t-enabled">Enabled</label>
                        <InputSwitch
                            id="rm-t-enabled"
                            checked={thresholdForm.enabled ?? true}
                            onChange={e => setThresholdForm(f => ({ ...f, enabled: e.value }))}
                        />
                    </div>
                </div>
            </Dialog>
        </div>
    );
};

export default RoomManagementView;
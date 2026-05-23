import "../styles/App.css";
import "primereact/resources/themes/lara-light-cyan/theme.css";
import "primeicons/primeicons.css";
import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { format, subDays, subHours, subYears } from "date-fns";
import type { Plugin } from "chart.js";
import { Card } from "primereact/card";
import { Chart } from "primereact/chart";
import { Column } from "primereact/column";
import { DataTable } from "primereact/datatable";
import { Dropdown } from "primereact/dropdown";
import { InputText } from "primereact/inputtext";
import { Message } from "primereact/message";
import { ProgressSpinner } from "primereact/progressspinner";
import { Tag } from "primereact/tag";
import { Toast } from "primereact/toast";

import NavbarComponent from "../components/NavbarComponent";
import {
    BuildingDTO,
    DepartmentDTO,
    GetAllViolationStatusEnum,
    MeasurementDTOMetricEnum,
    RoomDTO,
    RoomType,
    ThresholdDTO,
    ThresholdDTOThresholdTypeEnum,
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

const rangeStart = (range: TimeRange): Date => {
    const now = new Date();
    switch (range) {
        case "24h":
            return subHours(now, 24);
        case "7d":
            return subDays(now, 7);
        case "30d":
            return subDays(now, 30);
        case "90d":
            return subDays(now, 90);
        case "1y":
            return subYears(now, 1);
    }
};

const tickLabel = (timestamp: string, range: TimeRange): string => {
    const date = new Date(timestamp);
    if (range === "24h") return format(date, "HH:mm");
    if (range === "7d" || range === "30d") return format(date, "MM/dd HH:mm");
    return format(date, "MM/dd");
};

const formatNumber = (value?: number, digits = 1): string =>
    value === undefined || value === null ? "-" : value.toFixed(digits);

const metricLabel = (metric?: string): string =>
    METRICS.find(m => m.key === metric)?.label ?? metric?.replace("_", " ") ?? "-";

const RoomManagementView: React.FC = () => {
    const [rooms, setRooms] = useState<RoomDTO[]>([]);
    const [departments, setDepartments] = useState<DepartmentDTO[]>([]);
    const [buildings, setBuildings] = useState<BuildingDTO[]>([]);
    const [selectedRoomId, setSelectedRoomId] = useState<number | null>(null);
    const [roomFilter, setRoomFilter] = useState("");
    const [timeRange, setTimeRange] = useState<TimeRange>("24h");
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

        void loadRooms();
        BuildingService.getAll().then(setBuildings).catch(() => setBuildings([]));
        DepartmentService.getAll().then(setDepartments).catch(() => setDepartments([]));
    }, []);

    useEffect(() => {
        if (!selectedRoomId) return;

        const loadAnalytics = async () => {
            setLoadingAnalytics(true);
            setAnalyticsError(null);
            setPrivacyRestricted(false);
            const from = toLocalDateTimeParam(rangeStart(timeRange));
            const to = toLocalDateTimeParam(new Date());

            try {
                const [summaryData, trendData, violationSummaryData, activeViolations, resolvedViolations, thresholdData] =
                    await Promise.all([
                        AnalyticsService.getRoomSummary(selectedRoomId, from, to),
                        AnalyticsService.getRoomTrend(selectedRoomId, selectedMetric, from, to),
                        AnalyticsService.getRoomViolations(selectedRoomId),
                        ViolationService.getAll({
                            roomId: selectedRoomId,
                            violationStatus: GetAllViolationStatusEnum.ACTIVE,
                        }),
                        ViolationService.getAll({
                            roomId: selectedRoomId,
                            violationStatus: GetAllViolationStatusEnum.RESOLVED,
                        }),
                        ThresholdService.getAll({ roomId: selectedRoomId }),
                    ]);

                setSummary(summaryData);
                setTrend(trendData);
                setViolationSummary(violationSummaryData);
                setViolations([...activeViolations, ...resolvedViolations]);
                setThresholds(thresholdData);
            } catch (err: unknown) {
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
                setLoadingAnalytics(false);
            }
        };

        void loadAnalytics();
    }, [selectedRoomId, selectedMetric, timeRange]);

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

        const labels = points.map(point => tickLabel(point.timestamp!, timeRange));
        const datasets = [{
            label: metric.label,
            data: points.map(point => point.value ?? null),
            fill: false,
            borderColor: metric.color,
            backgroundColor: `${metric.color}33`,
            tension: 0.25,
            pointRadius: points.length > 160 ? 0 : 3,
            spanGaps: false,
        }];

        thresholds
            .filter(threshold =>
                threshold.metric === selectedMetric &&
                threshold.enabled !== false &&
                threshold.boundValue !== undefined
            )
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
            value={row.roomType === RoomType.COMMON_AREAS ? "Common area" : "Office"}
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

    return (
        <div>
            <NavbarComponent />
            <Toast ref={toast} />

            <div style={{ padding: "1.5rem 2rem" }}>
                <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: "1rem", marginBottom: "1rem" }}>
                    <div>
                        <h2 style={{ margin: 0 }}>Room Climate Data</h2>
                        <p style={{ margin: "0.35rem 0 0", color: "#64748b" }}>
                            Building administration view for persisted measurements, trends, and threshold violations.
                        </p>
                    </div>
                </div>

                <div style={{ display: "grid", gridTemplateColumns: "minmax(420px, 480px) minmax(0, 1fr)", gap: "1rem", alignItems: "start" }}>
                    <Card style={{ minWidth: 0 }}>
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
                                tableStyle={{ tableLayout: "fixed", width: "100%" }}
                            >
                                <Column field="name" header="Room" sortable style={{ width: "34%" }} />
                                <Column field="roomType" header="Type" body={roomTypeTemplate} sortable style={{ width: "28%" }} />
                                <Column header="Department" body={(row: RoomDTO) => getDepartmentName(row.departmentId)} sortable style={{ width: "38%" }} />
                            </DataTable>
                        </div>
                    </Card>

                    <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
                        {!selectedRoomId ? (
                            <Message severity="info" text="Select a room to view climate data." />
                        ) : (
                            <>
                                <Card>
                                    <div style={{ display: "flex", justifyContent: "space-between", gap: "1rem", flexWrap: "wrap", marginBottom: "1rem" }}>
                                        <div>
                                            <h3 style={{ margin: 0 }}>{selectedRoom?.name ?? `Room ${selectedRoomId}`}</h3>
                                            <p style={{ margin: "0.35rem 0 0", color: "#64748b" }}>
                                                {getBuildingName(selectedRoom?.buildingId)} - {getDepartmentName(selectedRoom?.departmentId)}
                                            </p>
                                        </div>
                                        <div style={{ display: "flex", gap: "0.75rem", flexWrap: "wrap" }}>
                                            <Dropdown
                                                value={timeRange}
                                                options={TIME_RANGES.map(range => ({
                                                    label: `${range.label} (${range.bucketHint})`,
                                                    value: range.value,
                                                }))}
                                                onChange={event => setTimeRange(event.value)}
                                                style={{ minWidth: "12rem" }}
                                            />
                                            <Dropdown
                                                value={selectedMetric}
                                                options={METRICS.map(metric => ({ label: metric.label, value: metric.key }))}
                                                onChange={event => setSelectedMetric(event.value)}
                                                style={{ minWidth: "12rem" }}
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
                                    ) : privacyRestricted || chartEmpty ? (
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
                                            <div style={{ height: "360px" }}>
                                                <Chart type="line" data={chartData} options={CHART_OPTIONS} plugins={[buildGapPlugin()]} style={{ height: "100%" }} />
                                            </div>
                                        </>
                                    )}
                                </Card>

                                <div style={{ display: "grid", gridTemplateColumns: "repeat(4, minmax(130px, 1fr))", gap: "1rem" }}>
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

                                    <DataTable
                                        value={violations}
                                        emptyMessage="No active or resolved threshold violations for this room."
                                        stripedRows
                                        paginator
                                        rows={8}
                                        sortField="startTime"
                                        sortOrder={-1}
                                    >
                                        <Column field="metric" header="Metric" body={(row: ThresholdViolationDTO) => metricLabel(row.metric)} sortable />
                                        <Column field="value" header="Value" body={(row: ThresholdViolationDTO) => formatNumber(row.value)} sortable />
                                        <Column field="violationStatus" header="Status" body={violationStatusTemplate} sortable />
                                        <Column field="startTime" header="Started" body={(row: ThresholdViolationDTO) => dateTemplate(row.startTime)} sortable />
                                        <Column field="endTime" header="Ended" body={(row: ThresholdViolationDTO) => dateTemplate(row.endTime)} sortable />
                                    </DataTable>
                                </Card>
                            </>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default RoomManagementView;

import React, { useCallback, useMemo, useRef, useState } from "react";
import { format, addDays, addYears } from "date-fns";
import { flushSync } from "react-dom";
import jsPDF from "jspdf";
import html2canvas from "html2canvas";

import { Button } from "primereact/button";
import { Checkbox } from "primereact/checkbox";
import { Dropdown } from "primereact/dropdown";
import { InputText } from "primereact/inputtext";
import { Message } from "primereact/message";
import { ProgressSpinner } from "primereact/progressspinner";
import { Sidebar } from "primereact/sidebar";
import { Tag } from "primereact/tag";
import { Toast } from "primereact/toast";

import { MeasurementDTOMetricEnum, RoomDTO, RoomType, GetAllViolationStatusEnum, ThresholdViolationDTO }
    from "../generated-skeleton-api";
import { AnalyticsService, RoomSummaryDTO, RoomTrendDTO } from "../services/AnalyticsService";
import { ViolationService } from "../services/ViolationService";

export interface ReportUser {
    email?: string;
    firstName?: string;
    lastName?: string;
}

export interface RoomClimateReportPanelProps {
    rooms: RoomDTO[];
    currentUser?: ReportUser | null;
    onSendReport: (email: string, pdfBlob: Blob) => Promise<void>;
}

type TimeRange = "24h" | "7d" | "30d" | "90d" | "1y";

const TIME_RANGES: { label: string; value: TimeRange }[] = [
    { label: "Last 24 hours", value: "24h" },
    { label: "Last 7 days",   value: "7d"  },
    { label: "Last 30 days",  value: "30d" },
    { label: "Last 90 days",  value: "90d" },
    { label: "Last year",     value: "1y"  },
];

const METRICS: { key: MeasurementDTOMetricEnum; label: string; unit: string; color: string }[] = [
    { key: MeasurementDTOMetricEnum.TEMPERATURE, label: "Temperature", unit: "°C",  color: "#f97316" },
    { key: MeasurementDTOMetricEnum.HUMIDITY,    label: "Humidity",    unit: "%",   color: "#2563eb" },
    { key: MeasurementDTOMetricEnum.PRESSURE,    label: "Pressure",    unit: "hPa", color: "#7c3aed" },
    { key: MeasurementDTOMetricEnum.IAQ,         label: "Air Quality", unit: "",    color: "#16a34a" },
];

const toParam = (d: Date) => format(d, "yyyy-MM-dd'T'HH:mm:ss");

function rangeWindow(range: TimeRange): { from: Date; to: Date } {
    const to  = new Date();
    const from = (() => {
        switch (range) {
            case "24h": return addDays(to, -1);
            case "7d":  return addDays(to, -7);
            case "30d": return addDays(to, -30);
            case "90d": return addDays(to, -90);
            case "1y":  return addYears(to, -1);
        }
    })();
    return { from, to };
}

const fmt2 = (v?: number) => (v === undefined || v === null ? "—" : v.toFixed(2));

function isValidEmail(email: string) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim());
}

interface RoomReportData {
    room: RoomDTO;
    summary: RoomSummaryDTO | null;
    trends: Partial<Record<MeasurementDTOMetricEnum, RoomTrendDTO>>;
    violations: ThresholdViolationDTO[];
    error?: string;
}

async function fetchRoomData(
    room: RoomDTO,
    metrics: MeasurementDTOMetricEnum[],
    range: TimeRange,
): Promise<RoomReportData> {
    const { from, to } = rangeWindow(range);
    const fromStr= toParam(from);
    const toStr = toParam(to);

    try {
        const [summary, activeViolations, resolvedViolations] = await Promise.all([
            AnalyticsService.getRoomSummary(room.id!, fromStr, toStr),
            ViolationService.getAll({ roomId: room.id, violationStatus: GetAllViolationStatusEnum.ACTIVE }),
            ViolationService.getAll({ roomId: room.id, violationStatus: GetAllViolationStatusEnum.RESOLVED }),
        ]);

        const trendEntries = await Promise.all(
            metrics.map(async metric => {
                const trend = await AnalyticsService.getRoomTrend(room.id!, metric, fromStr, toStr);
                return [metric, trend] as [MeasurementDTOMetricEnum, RoomTrendDTO];
            })
        );

        return {
            room,
            summary,
            trends: Object.fromEntries(trendEntries),
            violations: [...activeViolations, ...resolvedViolations],
        };
    } catch (err: unknown) {
        const status = (err as { response?: { status?: number } })?.response?.status;
        return {
            room,
            summary: null,
            trends: {},
            violations: [],
            error: status === 403
                ? "Access restricted (privacy mode or insufficient privileges)"
                : "Failed to fetch data",
        };
    }
}

async function buildPdfBlob(
    containerEl: HTMLElement,
    title: string,
): Promise<Blob> {
    const canvas = await html2canvas(containerEl, {
        scale: 2,
        useCORS: true,
        backgroundColor: "#ffffff",
        logging: false,
    });

    const imgData  = canvas.toDataURL("image/png");
    const pdfW     = 210;
    const pdfH     = (canvas.height * pdfW) / canvas.width;
    const pdf      = new jsPDF({ orientation: pdfH > pdfW ? "p" : "l", unit: "mm", format: "a4" });

    const pageH = pdf.internal.pageSize.getHeight();
    let yOffset = 0;

    while (yOffset < pdfH) {
        if (yOffset > 0) pdf.addPage();
        pdf.addImage(imgData, "PNG", 0, -yOffset, pdfW, pdfH);
        yOffset += pageH;
    }

    return pdf.output("blob");
}

interface PdfContentProps {
    dataList: RoomReportData[];
    metrics: MeasurementDTOMetricEnum[];
    range: TimeRange;
    generatedAt: Date;
}

const PdfContent = React.forwardRef<HTMLDivElement, PdfContentProps>(
    ({ dataList, metrics, range, generatedAt }, ref) => {
        const rangeLabel = TIME_RANGES.find(r => r.value === range)?.label ?? range;

        return (
            <div
                ref={ref}
        style={{
            width: "900px",
                padding: "40px 48px",
                fontFamily: "'Segoe UI', system-ui, sans-serif",
                backgroundColor: "#ffffff",
                color: "#1e293b",
        }}
    >
        <div style={{ borderBottom: "3px solid #0f172a", paddingBottom: "16px", marginBottom: "32px" }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-end" }}>
        <div>
            <div style={{ fontSize: "11px", fontWeight: 600, letterSpacing: "0.12em", textTransform: "uppercase", color: "#64748b", marginBottom: "4px" }}>
        Climate Report
        </div>
        <h1 style={{ margin: 0, fontSize: "26px", fontWeight: 700 }}>Room Climate Summary</h1>
        </div>
        <div style={{ textAlign: "right", fontSize: "12px", color: "#64748b" }}>
        <div>Period: <strong>{rangeLabel}</strong></div>
        <div>Generated: {format(generatedAt, "dd.MM.yyyy HH:mm")}</div>
        <div>Rooms included: <strong>{dataList.length}</strong></div>
        </div>
        </div>
        </div>

        {dataList.map((data, i) => (
            <div key={data.room.id} style={{ marginBottom: "40px", pageBreakInside: "avoid" }}>
            {/* Room title */}
            <div style={{
            display: "flex", alignItems: "center", gap: "12px",
                marginBottom: "16px",
                borderLeft: "4px solid #0f172a",
                paddingLeft: "12px",
        }}>
            <h2 style={{ margin: 0, fontSize: "18px", fontWeight: 700 }}>
            {i + 1}. {data.room.name}
            </h2>
            <span style={{
            fontSize: "11px", fontWeight: 600, padding: "2px 8px",
                borderRadius: "4px", background: "#f1f5f9", color: "#475569",
                textTransform: "uppercase", letterSpacing: "0.07em",
        }}>
            {data.room.roomType === RoomType.COMMON_AREAS ? "Common Area" : "Office"}
            </span>
            {data.room.privacyMode && (
                <span style={{
                fontSize: "11px", fontWeight: 600, padding: "2px 8px",
                    borderRadius: "4px", background: "#fef3c7", color: "#92400e",
                    textTransform: "uppercase", letterSpacing: "0.07em",
            }}>
                Privacy Mode
            </span>
            )}
            </div>

            {data.error ? (
                <div style={{
                padding: "16px", borderRadius: "6px",
                background: "#fef2f2", border: "1px solid #fecaca",
                color: "#991b1b", fontSize: "13px",
            }}>
            {data.error}
                </div>
            ) : (
                <>
                    {/* Metric summary grid */}
                <div style={{
                display: "grid", gridTemplateColumns: "repeat(4, 1fr)",
                    gap: "12px", marginBottom: "20px",
            }}>
                {metrics.map(metricKey => {
                    const def   = METRICS.find(m => m.key === metricKey)!;
                    const stats = data.summary?.metrics?.[metricKey];
                    return (
                        <div key={metricKey} style={{
                        border: "1px solid #e2e8f0", borderRadius: "8px",
                            padding: "14px 16px", background: "#f8fafc",
                    }}>
                    <div style={{ fontSize: "11px", color: "#64748b", fontWeight: 600, marginBottom: "6px", textTransform: "uppercase", letterSpacing: "0.08em" }}>
                    {def.label}
                    </div>
                    <div style={{ fontSize: "22px", fontWeight: 700, color: def.color, marginBottom: "4px" }}>
                    {fmt2(stats?.latest)}{def.unit && ` ${def.unit}`}
                    </div>
                    <div style={{ fontSize: "11px", color: "#94a3b8" }}>
                    avg {fmt2(stats?.avg)} · min {fmt2(stats?.min)} · max {fmt2(stats?.max)}
                    </div>
                    <div style={{ fontSize: "11px", color: "#94a3b8" }}>
                    {stats?.count ?? 0} readings
                    </div>
                    </div>
                );
                })}
                </div>

                {data.violations.length > 0 && (
                    <div style={{ marginTop: "12px" }}>
                    <div style={{ fontSize: "13px", fontWeight: 600, color: "#374151", marginBottom: "8px" }}>
                    Threshold Violations ({data.violations.length})
                </div>
                <table style={{ width: "100%", borderCollapse: "collapse", fontSize: "12px" }}>
                    <thead>
                        <tr style={{ background: "#f1f5f9" }}>
                    {["Metric", "Value", "Status", "Started", "Ended"].map(h => (
                        <th key={h} style={{ padding: "8px 12px", textAlign: "left", fontWeight: 600, color: "#374151", borderBottom: "1px solid #e2e8f0" }}>
                        {h}
                        </th>
                    ))}
                    </tr>
                    </thead>
                    <tbody>
                    {data.violations.slice(0, 20).map((v, vi) => (
                            <tr key={vi} style={{ borderBottom: "1px solid #f1f5f9" }}>
                    <td style={{ padding: "7px 12px" }}>{METRICS.find(m => m.key === v.metric)?.label ?? v.metric}</td>
                <td style={{ padding: "7px 12px" }}>{fmt2(v.value)}</td>
                <td style={{ padding: "7px 12px" }}>
                    <span style={{
                    padding: "2px 7px", borderRadius: "4px", fontSize: "11px", fontWeight: 600,
                        background: v.violationStatus === "ACTIVE" ? "#fef2f2" : "#f0fdf4",
                        color:      v.violationStatus === "ACTIVE" ? "#b91c1c"  : "#15803d",
                }}>
                    {v.violationStatus}
                    </span>
                    </td>
                    <td style={{ padding: "7px 12px" }}>{v.startTime ? format(new Date(v.startTime), "dd.MM.yy HH:mm") : "—"}</td>
                <td style={{ padding: "7px 12px" }}>{v.endTime   ? format(new Date(v.endTime),   "dd.MM.yy HH:mm") : "—"}</td>
                </tr>
                ))}
                    {data.violations.length > 20 && (
                        <tr>
                            <td colSpan={5} style={{ padding: "7px 12px", color: "#64748b", fontStyle: "italic" }}>
                    … and {data.violations.length - 20} more violations
                    </td>
                    </tr>
                    )}
                    </tbody>
                    </table>
                    </div>
                )}
                </>
            )}

            {i < dataList.length - 1 && (
                <div style={{ borderBottom: "1px dashed #e2e8f0", marginTop: "32px" }} />
            )}
            </div>
        ))}

        <div style={{ borderTop: "1px solid #e2e8f0", paddingTop: "12px", marginTop: "24px", fontSize: "11px", color: "#94a3b8", display: "flex", justifyContent: "space-between" }}>
        <span>Room Climate Report — {rangeLabel}</span>
        <span>Generated {format(generatedAt, "dd.MM.yyyy HH:mm:ss")}</span>
        </div>
        </div>
    );
    }
);
PdfContent.displayName = "PdfContent";

const RoomClimateReportPanel: React.FC<RoomClimateReportPanelProps> = ({
                                                                           rooms,
                                                                           currentUser,
                                                                           onSendReport,
                                                                       }) => {
    const [pdfData, setPdfData] = useState<RoomReportData[]>([]);
    const [isExpanded,      setIsExpanded     ] = useState(false);
    const [roomSearch,      setRoomSearch     ] = useState("");
    const [selectedRoomIds, setSelectedRoomIds] = useState<Set<number>>(new Set());
    const [selectedMetrics, setSelectedMetrics] = useState<Set<MeasurementDTOMetricEnum>>(
        new Set(METRICS.map(m => m.key))
    );
    const [range,           setRange          ] = useState<TimeRange>("24h");
    const [email,           setEmail          ] = useState("");
    const [emailError,      setEmailError     ] = useState<string | null>(null);

    const [status, setStatus] = useState<
        "idle" | "fetching" | "rendering" | "sending" | "done" | "error"
    >("idle");
    const [statusMessage,   setStatusMessage  ] = useState<string | null>(null);

    const pdfContainerRef = useRef<HTMLDivElement>(null);
    const toast           = useRef<Toast>(null);

    const filteredRooms = useMemo(() => {
        const q = roomSearch.trim().toLowerCase();
        if (!q) return rooms;
        return rooms.filter(r => r.name?.toLowerCase().includes(q));
    }, [rooms, roomSearch]);

    const toggleRoom = useCallback((id: number) => {
        setSelectedRoomIds(prev => {
            const next = new Set(prev);
            next.has(id) ? next.delete(id) : next.add(id);
            return next;
        });
    }, []);

    const toggleSelectAll = useCallback(() => {
        if (selectedRoomIds.size === filteredRooms.length) {
            setSelectedRoomIds(new Set());
        } else {
            setSelectedRoomIds(new Set(filteredRooms.map(r => r.id!)));
        }
    }, [filteredRooms, selectedRoomIds]);

    const toggleMetric = useCallback((key: MeasurementDTOMetricEnum) => {
        setSelectedMetrics(prev => {
            const next = new Set(prev);
            next.has(key) ? next.delete(key) : next.add(key);
            return next;
        });
    }, []);

    const useMyEmail = useCallback(() => {
        if (currentUser?.email) {
            setEmail(currentUser.email);
            setEmailError(null);
        }
    }, [currentUser]);

    const generatePdfBlob = useCallback(async (): Promise<Blob | null> => {
        if (selectedRoomIds.size === 0) {
            toast.current?.show({ severity: "warn", summary: "No rooms selected", detail: "Please select at least one room.", life: 3000 });
            return null;
        }
        if (selectedMetrics.size === 0) {
            toast.current?.show({ severity: "warn", summary: "No metrics selected", detail: "Please select at least one metric.", life: 3000 });
            return null;
        }

        const chosenRooms  = rooms.filter(r => r.id !== undefined && selectedRoomIds.has(r.id));
        const chosenMetrics = METRICS.map(m => m.key).filter(k => selectedMetrics.has(k));

        setStatus("fetching");
        setStatusMessage(`Fetching data for ${chosenRooms.length} room(s)…`);

        const dataList = await Promise.all(
            chosenRooms.map(room => fetchRoomData(room, chosenMetrics, range))
        );

        flushSync(() => {
            setPdfData(dataList);
        });

        setStatus("rendering");
        setStatusMessage("Rendering PDF…");

        await new Promise(resolve => setTimeout(resolve, 100));

        if (!pdfContainerRef.current) {
            setStatus("error");
            setStatusMessage("PDF render element not found.");
            return null;
        }

        const blob = await buildPdfBlob(pdfContainerRef.current, "Room Climate Report");
        return blob;
    }, [rooms, selectedRoomIds, selectedMetrics, range]);

    const handleDownload = useCallback(async () => {
        try {
            const blob = await generatePdfBlob();
            if (!blob) { setStatus("idle"); return; }

            const url  = URL.createObjectURL(blob);
            const link = document.createElement("a");
            link.href     = url;
            link.download = `room-climate-report-${format(new Date(), "yyyy-MM-dd-HHmm")}.pdf`;
            link.click();
            URL.revokeObjectURL(url);

            setStatus("done");
            setStatusMessage("PDF downloaded successfully.");
            toast.current?.show({ severity: "success", summary: "Downloaded", detail: "Report PDF saved.", life: 3000 });
        } catch {
            setStatus("error");
            setStatusMessage("Failed to generate PDF. Please try again.");
        }
    }, [generatePdfBlob]);

    const handleSendEmail = useCallback(async () => {
        const trimmed = email.trim();
        if (!isValidEmail(trimmed)) {
            setEmailError("Please enter a valid email address.");
            return;
        }
        setEmailError(null);

        try {
            const blob = await generatePdfBlob();
            if (!blob) { setStatus("idle"); return; }

            setStatus("sending");
            setStatusMessage(`Sending to ${trimmed}…`);

            await onSendReport(trimmed, blob);

            setStatus("done");
            setStatusMessage(`Report sent to ${trimmed}.`);
            toast.current?.show({ severity: "success", summary: "Email sent", detail: `Report delivered to ${trimmed}`, life: 4000 });
        } catch {
            setStatus("error");
            setStatusMessage("Failed to send email. Please try again.");
        }
    }, [email, generatePdfBlob, onSendReport]);

    const isBusy = ["fetching", "rendering", "sending"].includes(status);

    const chosenMetrics = METRICS.map(m => m.key).filter(k => selectedMetrics.has(k));

    return (
        <>
            <Toast ref={toast} />

            {isBusy && (
                <div
                    aria-hidden="true"
                    style={{
                        position: "fixed",
                        top: "-99999px",
                        left: "-99999px",
                        width: "900px",
                        zIndex: -1,
                        pointerEvents: "none",
                    }}
                >
                    <PdfContent
                        ref={pdfContainerRef}
                        dataList={pdfData}
                        metrics={chosenMetrics}
                        range={range}
                        generatedAt={new Date()}
                    />
                </div>
            )}

            <Button
                icon="pi pi-file-pdf"
                rounded
                onClick={() => setIsExpanded(true)}
                style={{
                    width: "2.5rem",
                    height: "2.5rem"
                }}
            />

            <Sidebar
                visible={isExpanded}
                position="right"
                onHide={() => setIsExpanded(false)}
                style={{ width: "42rem", maxWidth: "100vw" }}
                header={
                    <div>
                        <h3 style={{ margin: 0 }}>Generate Climate Report</h3>
                        <p
                            style={{
                                margin: "0.25rem 0 0",
                                color: "#64748b",
                                fontSize: "0.875rem",
                            }}
                        >
                            Export a PDF summary for one or more rooms and optionally email it.
                        </p>
                    </div>
                }
            >
                <div
                    style={{
                        display: "flex",
                        flexDirection: "column",
                        gap: "1.5rem",
                        paddingTop: "0.5rem",
                    }}
                >

                    {/* ─ Row 1: Rooms + Metrics ─ */}
                    <div
                        style={{
                            display: "grid",
                            gridTemplateColumns: "1fr 1fr",
                            gap: "1.5rem",
                        }}
                    >

                        {/* Room selector */}
                        <div>
                            <div
                                style={{
                                    fontWeight: 600,
                                    fontSize: "0.875rem",
                                    marginBottom: "0.5rem",
                                    color: "#374151",
                                }}
                            >
                                Rooms
                                <span
                                    style={{
                                        fontWeight: 400,
                                        color: "#94a3b8",
                                        marginLeft: "0.5rem",
                                    }}
                                >
                                {selectedRoomIds.size} selected
                            </span>
                            </div>

                            <InputText
                                value={roomSearch}
                                onChange={e => setRoomSearch(e.target.value)}
                                placeholder="Search rooms…"
                                style={{ width: "100%", marginBottom: "0.5rem" }}
                            />

                            <div
                                style={{
                                    display: "flex",
                                    alignItems: "center",
                                    gap: "0.5rem",
                                    padding: "0.4rem 0.6rem",
                                    cursor: "pointer",
                                    borderBottom: "1px solid #f1f5f9",
                                    marginBottom: "0.25rem",
                                }}
                                onClick={toggleSelectAll}
                            >
                                <Checkbox
                                    checked={
                                        filteredRooms.length > 0 &&
                                        filteredRooms.every(
                                            r =>
                                                r.id !== undefined &&
                                                selectedRoomIds.has(r.id)
                                        )
                                    }
                                    onChange={toggleSelectAll}
                                    inputId="select-all-rooms"
                                />

                                <label
                                    htmlFor="select-all-rooms"
                                    style={{
                                        cursor: "pointer",
                                        fontSize: "0.8rem",
                                        color: "#64748b",
                                        fontWeight: 600,
                                    }}
                                >
                                    {selectedRoomIds.size === filteredRooms.length &&
                                    filteredRooms.length > 0
                                        ? "Deselect all"
                                        : "Select all"}
                                </label>
                            </div>

                            <div
                                style={{
                                    maxHeight: "220px",
                                    overflowY: "auto",
                                    border: "1px solid #e2e8f0",
                                    borderRadius: "6px",
                                }}
                            >
                                {filteredRooms.length === 0 ? (
                                    <div
                                        style={{
                                            padding: "1rem",
                                            color: "#94a3b8",
                                            fontSize: "0.875rem",
                                            textAlign: "center",
                                        }}
                                    >
                                        No rooms match your search.
                                    </div>
                                ) : (
                                    filteredRooms.map(room => (
                                        <div
                                            key={room.id}
                                            onClick={() =>
                                                room.id !== undefined &&
                                                toggleRoom(room.id)
                                            }
                                            style={{
                                                display: "flex",
                                                alignItems: "center",
                                                gap: "0.625rem",
                                                padding: "0.5rem 0.75rem",
                                                cursor: "pointer",
                                                backgroundColor:
                                                    room.id !== undefined &&
                                                    selectedRoomIds.has(room.id)
                                                        ? "#eff6ff"
                                                        : "transparent",
                                                borderBottom:
                                                    "1px solid #f8fafc",
                                                transition: "background 0.12s",
                                            }}
                                        >
                                            <Checkbox
                                                checked={
                                                    room.id !== undefined &&
                                                    selectedRoomIds.has(room.id)
                                                }
                                                onChange={() =>
                                                    room.id !== undefined &&
                                                    toggleRoom(room.id)
                                                }
                                                inputId={`room-${room.id}`}
                                                onClick={e => e.stopPropagation()}
                                            />

                                            <label
                                                htmlFor={`room-${room.id}`}
                                                style={{
                                                    cursor: "pointer",
                                                    fontSize: "0.875rem",
                                                    flex: 1,
                                                }}
                                                onClick={e => e.stopPropagation()}
                                            >
                                                {room.name}
                                            </label>

                                            <Tag
                                                value={
                                                    room.roomType ===
                                                    RoomType.COMMON_AREAS
                                                        ? "Common"
                                                        : "Office"
                                                }
                                                severity={
                                                    room.roomType ===
                                                    RoomType.OFFICE
                                                        ? "info"
                                                        : "warning"
                                                }
                                                style={{ fontSize: "0.7rem" }}
                                            />
                                        </div>
                                    ))
                                )}
                            </div>
                        </div>

                        <div
                            style={{
                                display: "flex",
                                flexDirection: "column",
                                gap: "1.25rem",
                            }}
                        >
                            <div>
                                <div
                                    style={{
                                        fontWeight: 600,
                                        fontSize: "0.875rem",
                                        marginBottom: "0.5rem",
                                        color: "#374151",
                                    }}
                                >
                                    Metrics to include
                                </div>

                                <div
                                    style={{
                                        display: "flex",
                                        flexDirection: "column",
                                        gap: "0.5rem",
                                    }}
                                >
                                    {METRICS.map(m => (
                                        <div
                                            key={m.key}
                                            onClick={() => toggleMetric(m.key)}
                                            style={{
                                                display: "flex",
                                                alignItems: "center",
                                                gap: "0.625rem",
                                                cursor: "pointer",
                                                padding: "0.4rem 0.6rem",
                                                borderRadius: "6px",
                                                background: selectedMetrics.has(
                                                    m.key
                                                )
                                                    ? "#f8fafc"
                                                    : "transparent",
                                                border: `1px solid ${
                                                    selectedMetrics.has(m.key)
                                                        ? m.color + "55"
                                                        : "transparent"
                                                }`,
                                                transition: "all 0.12s",
                                            }}
                                        >
                                            <Checkbox
                                                checked={selectedMetrics.has(m.key)}
                                                onChange={() => toggleMetric(m.key)}
                                                inputId={`metric-${m.key}`}
                                                onClick={e => e.stopPropagation()}
                                            />

                                            <div
                                                style={{
                                                    width: "10px",
                                                    height: "10px",
                                                    borderRadius: "50%",
                                                    background: m.color,
                                                    flexShrink: 0,
                                                }}
                                            />

                                            <label
                                                htmlFor={`metric-${m.key}`}
                                                style={{
                                                    cursor: "pointer",
                                                    fontSize: "0.875rem",
                                                }}
                                                onClick={e => e.stopPropagation()}
                                            >
                                                {m.label}
                                                <span
                                                    style={{
                                                        color: "#94a3b8",
                                                        marginLeft: "0.3rem",
                                                    }}
                                                >
                                                {m.unit && `(${m.unit})`}
                                            </span>
                                            </label>
                                        </div>
                                    ))}
                                </div>
                            </div>

                            {/* Time range */}
                            <div>
                                <div
                                    style={{
                                        fontWeight: 600,
                                        fontSize: "0.875rem",
                                        marginBottom: "0.5rem",
                                        color: "#374151",
                                    }}
                                >
                                    Time range
                                </div>

                                <Dropdown
                                    value={range}
                                    options={TIME_RANGES}
                                    onChange={e => setRange(e.value)}
                                    style={{ width: "100%" }}
                                />
                            </div>
                        </div>
                    </div>

                    <div
                        style={{
                            display: "grid",
                            gridTemplateColumns: "1fr 1fr",
                            gap: "1.5rem",
                            borderTop: "1px solid #f1f5f9",
                            paddingTop: "1.25rem",
                        }}
                    >
                        <div
                            style={{
                                display: "flex",
                                flexDirection: "column",
                                gap: "0.75rem",
                            }}
                        >
                            <div
                                style={{
                                    fontWeight: 600,
                                    fontSize: "0.875rem",
                                    color: "#374151",
                                }}
                            >
                                <i
                                    className="pi pi-download"
                                    style={{ marginRight: "0.4rem" }}
                                />
                                Download PDF
                            </div>

                            <p
                                style={{
                                    margin: 0,
                                    fontSize: "0.8rem",
                                    color: "#64748b",
                                }}
                            >
                                Generate and immediately download the report to your device.
                            </p>

                            <Button
                                label={isBusy ? "Generating…" : "Download PDF"}
                                icon={isBusy ? undefined : "pi pi-file-pdf"}
                                severity="secondary"
                                onClick={handleDownload}
                                disabled={
                                    isBusy ||
                                    selectedRoomIds.size === 0 ||
                                    selectedMetrics.size === 0
                                }
                                style={{ alignSelf: "flex-start" }}
                            >
                                {isBusy && (
                                    <ProgressSpinner
                                        style={{
                                            width: "18px",
                                            height: "18px",
                                            marginRight: "0.5rem",
                                        }}
                                        strokeWidth="4"
                                    />
                                )}
                            </Button>
                        </div>

                        <div
                            style={{
                                display: "flex",
                                flexDirection: "column",
                                gap: "0.75rem",
                            }}
                        >
                            <div
                                style={{
                                    fontWeight: 600,
                                    fontSize: "0.875rem",
                                    color: "#374151",
                                }}
                            >
                                <i
                                    className="pi pi-envelope"
                                    style={{ marginRight: "0.4rem" }}
                                />
                                Send by Email
                            </div>

                            <p
                                style={{
                                    margin: 0,
                                    fontSize: "0.8rem",
                                    color: "#64748b",
                                }}
                            >
                                Attach the PDF to an email and send it to any address.
                            </p>

                            <div
                                style={{
                                    display: "flex",
                                    gap: "0.5rem",
                                    alignItems: "flex-start",
                                    flexWrap: "wrap",
                                }}
                            >
                                <div style={{ flex: 1, minWidth: "200px" }}>
                                    <InputText
                                        value={email}
                                        onChange={e => {
                                            setEmail(e.target.value);
                                            setEmailError(null);
                                        }}
                                        placeholder="recipient@example.com"
                                        style={{ width: "100%" }}
                                        className={emailError ? "p-invalid" : undefined}
                                    />

                                    {emailError && (
                                        <small
                                            style={{
                                                color: "#dc2626",
                                                fontSize: "0.775rem",
                                                display: "block",
                                                marginTop: "4px",
                                            }}
                                        >
                                            {emailError}
                                        </small>
                                    )}
                                </div>

                                {currentUser?.email && (
                                    <Button
                                        label="Use my email"
                                        icon="pi pi-user"
                                        size="small"
                                        severity="secondary"
                                        outlined
                                        onClick={useMyEmail}
                                        title={`Fill in ${currentUser.email}`}
                                        style={{
                                            flexShrink: 0,
                                            alignSelf: "flex-start",
                                        }}
                                    />
                                )}
                            </div>

                            <Button
                                label={
                                    status === "sending"
                                        ? "Sending…"
                                        : "Send Report"
                                }
                                icon={
                                    status === "sending"
                                        ? undefined
                                        : "pi pi-send"
                                }
                                onClick={handleSendEmail}
                                disabled={
                                    isBusy ||
                                    selectedRoomIds.size === 0 ||
                                    selectedMetrics.size === 0
                                }
                                style={{ alignSelf: "flex-start" }}
                            >
                                {status === "sending" && (
                                    <ProgressSpinner
                                        style={{
                                            width: "18px",
                                            height: "18px",
                                            marginRight: "0.5rem",
                                        }}
                                        strokeWidth="4"
                                    />
                                )}
                            </Button>
                        </div>
                    </div>

                    {/* Status feedback */}
                    {statusMessage && (
                        <Message
                            severity={
                                status === "error"
                                    ? "error"
                                    : status === "done"
                                        ? "success"
                                        : "info"
                            }
                            text={statusMessage}
                        />
                    )}

                    {selectedRoomIds.size === 0 && (
                        <Message
                            severity="warn"
                            text="Select at least one room to generate a report."
                        />
                    )}
                </div>
            </Sidebar>
        </>
    );
};

export default RoomClimateReportPanel;
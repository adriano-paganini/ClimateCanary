import '../styles/App.css';
import '../styles/AbsenceCalendar.css';
import 'primeicons/primeicons.css';
import React, { useCallback, useEffect, useState } from 'react';
import { useLocation, useParams, useNavigate } from 'react-router-dom';
import { Button } from 'primereact/button';
import { Chart } from 'primereact/chart';
import { Message } from 'primereact/message';
import { ProgressSpinner } from 'primereact/progressspinner';
import { addDays, format } from 'date-fns';

import type { Plugin } from 'chart.js';

import NavbarComponent from '../components/NavbarComponent';
import NoDataOverlay from '../components/NoDataOverlay';
import { useUser } from '../Contexts/AuthenticatedUserContext';
import { findGapRanges } from '../utilities/dataGapUtils';
import { buildRoomHistoryChartData, TrendPoint } from '../utilities/roomHistoryChartData';
import { AnalyticsService } from '../services/AnalyticsService';
import { ThresholdService } from '../services/ThresholdService';
import { RoomService } from '../services/RoomService';
import {
    MeasurementDTOMetricEnum,
    RoomDTO,
    ThresholdDTO,
} from '../generated-skeleton-api';

import { ROUTES } from '../utilities/routes.paths';

const METRICS: { key: MeasurementDTOMetricEnum; label: string; unit: string; color: string }[] = [
    { key: MeasurementDTOMetricEnum.TEMPERATURE, label: 'Temperature',      unit: '°C',  color: '#f97316' },
    { key: MeasurementDTOMetricEnum.HUMIDITY,    label: 'Humidity',         unit: '%',   color: '#3b82f6' },
    { key: MeasurementDTOMetricEnum.PRESSURE,    label: 'Pressure',         unit: 'hPa', color: '#8b5cf6' },
    { key: MeasurementDTOMetricEnum.IAQ,         label: 'Air Quality (IAQ)', unit: '',   color: '#22c55e' },
];

function dateOnly(date: Date): string {
    return format(date, 'yyyy-MM-dd');
}

function dateFromInput(value: string): Date | null {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return null;
    const date = new Date(`${value}T00:00:00`);
    return Number.isNaN(date.getTime()) ? null : date;
}

function dayStart(date: Date): Date {
    return new Date(date.getFullYear(), date.getMonth(), date.getDate());
}

function dayWindow(date: Date): { from: Date; to: Date } {
    const from = dayStart(date);
    return { from, to: addDays(from, 1) };
}

function visibleDayLabel(date: Date): string {
    return format(dayStart(date), 'dd.MM.yyyy');
}

const CHART_OPTIONS = {
    responsive: true,
    maintainAspectRatio: false,
    animation: false as const,
    plugins: {
        legend: { position: 'top' as const, labels: { boxWidth: 12, font: { size: 11 } } },
    },
    scales: {
        x: { ticks: { maxTicksLimit: 8, maxRotation: 0, font: { size: 10 } } },
        y: { ticks: { font: { size: 10 } } },
    },
};

// Draws a thin vertical line from the x-axis to each data point so values are easy to trace.
// Only active when there are ≤100 points (dense series would just look like a fill).
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const dropLinePlugin: any = {
    id: 'dropLines',
    afterDatasetsDraw(chart: any) {
        const { ctx, chartArea: { bottom } } = chart;
        const meta = chart.getDatasetMeta(0);
        if (!meta || meta.hidden || meta.data.length > 100) return;
        ctx.save();
        ctx.strokeStyle = 'rgba(59, 130, 246, 0.2)';
        ctx.lineWidth = 0.75;
        ctx.setLineDash([]);
        meta.data.forEach((el: any) => {
            if (!el.skip) {
                ctx.beginPath();
                ctx.moveTo(el.x, bottom);
                ctx.lineTo(el.x, el.y);
                ctx.stroke();
            }
        });
        ctx.restore();
    },
};

const RoomHistory: React.FC = () => {
    const { roomId } = useParams<{ roomId: string }>();
    const navigate  = useNavigate();
    const { pathname } = useLocation();
    const numId     = roomId ? parseInt(roomId, 10) : NaN;
    const openedFromDepartment = pathname.startsWith('/department/rooms/');

    const [currentDate, setCurrentDate] = useState(new Date());
    const [room,       setRoom]       = useState<RoomDTO | null>(null);
    const [trendsMap,  setTrendsMap]  = useState<Record<string, TrendPoint[]>>({});
    const [thresholds, setThresholds] = useState<ThresholdDTO[]>([]);
    const [loading,    setLoading]    = useState(true);
    const [error,      setError]      = useState<string | null>(null);
    const [privacyRestricted, setPrivacyRestricted] = useState(false);

    useEffect(() => {
        if (isNaN(numId)) return;
        RoomService.getById(numId).then(setRoom).catch(() => {});
        ThresholdService.getAll({ roomId: numId }).then(setThresholds).catch(() => {});
    }, [numId]);

    const loadMeasurements = useCallback(async () => {
        if (isNaN(numId)) return;
        setLoading(true);
        setError(null);
        setPrivacyRestricted(false);
        const window = dayWindow(currentDate);
        const from = format(window.from, "yyyy-MM-dd'T'HH:mm:ss");
        const to   = format(window.to, "yyyy-MM-dd'T'HH:mm:ss");
        try {
            const results = await Promise.all(
                METRICS.map(({ key }) =>
                    AnalyticsService.getRoomTrend(numId, key, from, to)
                        .then(data => ({
                            metric: key,
                            points: (data.points ?? [])
                                .filter((point): point is TrendPoint => point.timestamp !== undefined && point.value !== undefined),
                        }))
                )
            );
            const map: Record<string, TrendPoint[]> = {};
            for (const { metric, points } of results) {
                map[metric] = points;
            }
            setTrendsMap(map);
        } catch (err: unknown) {
            const status = (err as { response?: { status?: number } })?.response?.status;
            if (status === 403) {
                setPrivacyRestricted(true);
            } else {
                setError('Failed to load measurements.');
            }
        } finally {
            setLoading(false);
        }
    }, [numId, currentDate]);

    useEffect(() => { void loadMeasurements(); }, [loadMeasurements]);

    function buildChartData(metric: MeasurementDTOMetricEnum) {
        const points = trendsMap[metric] ?? [];
        return buildRoomHistoryChartData(metric, points, thresholds);
    }

    function navigateDay(direction: -1 | 1) {
        setCurrentDate(addDays(dayStart(currentDate), direction));
    }

    function jumpToDate(value: string) {
        const nextDate = dateFromInput(value);
        if (nextDate) {
            setCurrentDate(nextDate);
        }
    }

    function buildGapPlugin(metric: MeasurementDTOMetricEnum): Plugin {
        const points = trendsMap[metric] ?? [];
        const gaps = findGapRanges(points);
        return {
            id: `gapHighlight-${metric}`,
            beforeDraw(chart) {
                if (gaps.length === 0) return;
                const ctx = chart.ctx;
                const xScale = chart.scales['x'];
                const yScale = chart.scales['y'];
                ctx.save();
                ctx.fillStyle = 'rgba(156, 163, 175, 0.25)';
                for (const { startIdx, endIdx } of gaps) {
                    const x1 = xScale.getPixelForValue(startIdx);
                    const x2 = xScale.getPixelForValue(endIdx);
                    ctx.fillRect(x1, yScale.top, x2 - x1, yScale.bottom - yScale.top);
                }
                ctx.restore();
            },
        };
    }

    if (isNaN(numId)) {
        return (
            <div>
                <NavbarComponent />
                <div className="m-4"><Message severity="error" text="Invalid room ID." /></div>
            </div>
        );
    }

    return (
        <div>
            <NavbarComponent />
            <div style={{ padding: '1.5rem 2rem' }}>

                {/* Header row */}
                <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '1.5rem' }}>
                    <Button
                        icon="pi pi-arrow-left"
                        label="Back"
                        className="p-button-text"
                        onClick={() => navigate(
                            openedFromDepartment
                                ? ROUTES.DEPARTMENT_DASHBOARD
                                : ROUTES.DASHBOARD,
                        )}
                    />
                    <h2 style={{ margin: 0, color: '#111827' }}>
                        {room?.name ? `${room.name} — History` : `Room ${roomId} — History`}
                    </h2>
                </div>

                {/* Privacy banner */}
                {room?.privacyMode && (
                    <div style={{
                        display: 'flex', alignItems: 'center', gap: '0.6rem',
                        padding: '0.65rem 1rem', marginBottom: '1.25rem',
                        backgroundColor: '#f3f4f6', border: '1px solid #d1d5db',
                        borderRadius: '6px', color: '#374151', fontSize: '0.9rem',
                    }}>
                        <i className="pi pi-lock" style={{ color: '#6b7280' }} />
                        <span>Datenschutz aktiv — Messdaten werden nur bei ausreichender Raumbelegung (mind. 5 Personen) erfasst. Graue Bereiche zeigen Perioden ohne Daten.</span>
                    </div>
                )}

                <div className="absence-calendar-header" style={{ gridTemplateColumns: 'auto 1fr auto' }}>
                    <div className="absence-calendar-navigation">
                        <button type="button" onClick={() => navigateDay(-1)} aria-label="Previous day">
                            <i className="pi pi-chevron-left" aria-hidden="true" />
                        </button>
                        <button type="button" onClick={() => setCurrentDate(new Date())}>
                            Today
                        </button>
                        <button type="button" onClick={() => navigateDay(1)} aria-label="Next day">
                            <i className="pi pi-chevron-right" aria-hidden="true" />
                        </button>
                    </div>

                    <div className="absence-calendar-range">
                        {visibleDayLabel(currentDate)}
                    </div>

                    <div className="absence-date-jump">
                        <label htmlFor="room-history-jump-date">Jump to</label>
                        <input
                            id="room-history-jump-date"
                            type="date"
                            value={dateOnly(currentDate)}
                            onChange={event => jumpToDate(event.target.value)}
                        />
                    </div>
                </div>

                {error && <Message severity="error" text={error} style={{ marginBottom: '1rem', display: 'block' }} />}
                {privacyRestricted && (
                    <Message severity="warn" text="Klimadaten nicht verfügbar — Datenschutz aktiv (Belegung unter Mindestanzahl)." style={{ marginBottom: '1rem', display: 'block' }} />
                )}

                {loading ? (
                    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '40vh' }}>
                        <ProgressSpinner />
                    </div>
                ) : (
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr', gap: '1.5rem' }}>
                        {METRICS.map(({ key, label, unit }) => {
                            const data    = buildChartData(key);
                            const isEmpty = (data.datasets[0].data as (number | null)[]).every(v => v === null);
                            const hasThresholds = data.datasets.length > 1;
                            return (
                                <div
                                    key={key}
                                    style={{
                                        background:   '#fff',
                                        border:       '1px solid #e5e7eb',
                                        borderRadius: '8px',
                                        padding:      '1.25rem',
                                    }}
                                >
                                    <h3 style={{ margin: '0 0 1rem', color: '#374151', fontSize: '1rem', fontWeight: 600 }}>
                                        {label}{unit && ` (${unit})`}
                                    </h3>
                                    {privacyRestricted || (isEmpty && !hasThresholds) ? (
                                        <NoDataOverlay
                                            message={privacyRestricted ? 'Datenschutz aktiv — keine Daten verfügbar' : 'Keine Daten verfügbar'}
                                            icon={privacyRestricted ? 'pi pi-lock' : 'pi pi-ban'}
                                        />
                                    ) : (
                                        <div style={{ height: '220px' }}>
                                            <Chart
                                                type="line"
                                                data={data}
                                                options={CHART_OPTIONS}
                                                plugins={[buildGapPlugin(key)]}
                                                style={{ height: '100%' }}
                                            />
                                        </div>
                                    )}
                                </div>
                            );
                        })}
                    </div>
                )}
            </div>
        </div>
    );
};

export default RoomHistory;

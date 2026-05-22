import '../styles/App.css';
import 'primeicons/primeicons.css';
import React, { useCallback, useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Button } from 'primereact/button';
import { Chart } from 'primereact/chart';
import { Message } from 'primereact/message';
import { ProgressSpinner } from 'primereact/progressspinner';
import { format, subDays, subHours } from 'date-fns';
import type { Plugin } from 'chart.js';

import globalAxios from 'axios';

import NavbarComponent from '../components/NavbarComponent';
import NoDataOverlay from '../components/NoDataOverlay';
import { ThresholdService } from '../services/ThresholdService';
import { RoomService } from '../services/RoomService';
import { findGapRanges } from '../utilities/dataGapUtils';
import {
    MeasurementDTOMetricEnum,
    RoomDTO,
    ThresholdDTO,
    ThresholdDTOThresholdTypeEnum,
} from '../generated-skeleton-api';

interface TrendPoint {
    timestamp: string;
    value: number;
}

interface RoomTrendDTO {
    roomId: number;
    metric: string;
    bucketSize: string;
    granularityReduced: boolean;
    points: TrendPoint[];
}
import { ROUTES } from '../utilities/routes.paths';

type TimeRange = '24h' | '7d' | '30d';

const TIME_RANGES: { label: string; value: TimeRange }[] = [
    { label: '24h',     value: '24h' },
    { label: '7 days',  value: '7d'  },
    { label: '30 days', value: '30d' },
];

const METRICS: { key: MeasurementDTOMetricEnum; label: string; unit: string; color: string }[] = [
    { key: MeasurementDTOMetricEnum.TEMPERATURE, label: 'Temperature',      unit: '°C',  color: '#f97316' },
    { key: MeasurementDTOMetricEnum.HUMIDITY,    label: 'Humidity',         unit: '%',   color: '#3b82f6' },
    { key: MeasurementDTOMetricEnum.PRESSURE,    label: 'Pressure',         unit: 'hPa', color: '#8b5cf6' },
    { key: MeasurementDTOMetricEnum.IAQ,         label: 'Air Quality (IAQ)', unit: '',   color: '#22c55e' },
];

function rangeStart(range: TimeRange): Date {
    const now = new Date();
    if (range === '24h') return subHours(now, 24);
    if (range === '7d')  return subDays(now, 7);
    return subDays(now, 30);
}

function tickLabel(ts: string, range: TimeRange): string {
    const d = new Date(ts);
    if (range === '24h') return format(d, 'HH:mm');
    if (range === '7d')  return format(d, 'MM/dd HH:mm');
    return format(d, 'MM/dd');
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

const RoomHistory: React.FC = () => {
    const { roomId } = useParams<{ roomId: string }>();
    const navigate  = useNavigate();
    const numId     = roomId ? parseInt(roomId, 10) : NaN;

    const [timeRange,         setTimeRange]         = useState<TimeRange>('24h');
    const [room,              setRoom]              = useState<RoomDTO | null>(null);
    const [trendsMap,         setTrendsMap]         = useState<Record<string, TrendPoint[]>>({});
    const [thresholds,        setThresholds]        = useState<ThresholdDTO[]>([]);
    const [loading,           setLoading]           = useState(true);
    const [error,             setError]             = useState<string | null>(null);
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
        const from = format(rangeStart(timeRange), "yyyy-MM-dd'T'HH:mm:ss");
        const to   = format(new Date(), "yyyy-MM-dd'T'HH:mm:ss");
        try {
            const results = await Promise.all(
                METRICS.map(({ key }) =>
                    globalAxios.get<RoomTrendDTO>(`/api/analytics/rooms/${numId}/trends`, {
                        params: { metric: key, from, to },
                    }).then(r => ({ metric: key, points: r.data.points }))
                )
            );
            const map: Record<string, TrendPoint[]> = {};
            for (const { metric, points } of results) {
                map[metric] = points;
            }
            setTrendsMap(map);
        } catch (err: unknown) {
            if (
                err !== null &&
                typeof err === 'object' &&
                'response' in err &&
                (err as { response?: { status?: number } }).response?.status === 403
            ) {
                setPrivacyRestricted(true);
            } else {
                setError('Failed to load measurements.');
            }
        } finally {
            setLoading(false);
        }
    }, [numId, timeRange]);

    useEffect(() => { void loadMeasurements(); }, [loadMeasurements]);

    function buildChartData(metric: MeasurementDTOMetricEnum, color: string) {
        const points = trendsMap[metric] ?? [];

        const labels = points.map(p => tickLabel(p.timestamp, timeRange));
        const values = points.map(p => p.value ?? null);

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const datasets: any[] = [{
            label:           metric,
            data:            values,
            fill:            false,
            borderColor:     color,
            backgroundColor: color + '33',
            tension:         0.3,
            pointRadius:     points.length > 200 ? 0 : 3,
            spanGaps:        false,
        }];

        // Threshold dashed lines
        const activeThresholds = thresholds.filter(t => t.metric === metric && t.enabled !== false && t.boundValue !== undefined);
        for (const t of activeThresholds) {
            const isUpper = t.thresholdType === ThresholdDTOThresholdTypeEnum.UPPER;
            datasets.push({
                label:       isUpper ? 'Upper limit' : 'Lower limit',
                data:        Array(labels.length).fill(t.boundValue),
                borderColor: isUpper ? '#ef4444' : '#f59e0b',
                borderDash:  [6, 4],
                borderWidth: 2,
                pointRadius: 0,
                fill:        false,
                tension:     0,
            });
        }

        return { labels, datasets };
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
                        onClick={() => navigate(ROUTES.DASHBOARD)}
                    />
                    <h2 style={{ margin: 0, color: '#111827' }}>
                        {room?.name ? `${room.name} — History` : `Room ${roomId} — History`}
                    </h2>
                </div>

                {/* Privacy mode banner — shown when room is currently below minimum occupancy (Option B: privacyMode = belowMinOccupancy) */}
                {room?.privacyMode && (
                    <div style={{
                        display:         'flex',
                        alignItems:      'center',
                        gap:             '0.6rem',
                        padding:         '0.65rem 1rem',
                        marginBottom:    '1.25rem',
                        backgroundColor: '#f3f4f6',
                        border:          '1px solid #d1d5db',
                        borderRadius:    '6px',
                        color:           '#374151',
                        fontSize:        '0.9rem',
                    }}>
                        <i className="pi pi-lock" style={{ color: '#6b7280' }} />
                        <span>Datenschutz aktiv — Aktuelle Belegung unter Mindestanzahl. Klimadaten sind für diesen Zeitraum eingeschränkt. Graue Bereiche zeigen Perioden ohne Daten.</span>
                    </div>
                )}

                {/* Time range buttons */}
                <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '2rem' }}>
                    {TIME_RANGES.map(r => (
                        <Button
                            key={r.value}
                            label={r.label}
                            size="small"
                            outlined={timeRange !== r.value}
                            onClick={() => setTimeRange(r.value)}
                        />
                    ))}
                </div>

                {error && <Message severity="error" text={error} style={{ marginBottom: '1rem', display: 'block' }} />}
                {privacyRestricted && (
                    <Message
                        severity="warn"
                        text="Klimadaten nicht verfügbar — Datenschutz aktiv (Belegung unter Mindestanzahl)."
                        style={{ marginBottom: '1rem', display: 'block' }}
                    />
                )}

                {loading ? (
                    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '40vh' }}>
                        <ProgressSpinner />
                    </div>
                ) : (
                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '1.5rem' }}>
                        {METRICS.map(({ key, label, unit, color }) => {
                            const data    = buildChartData(key, color);
                            const isEmpty = (data.datasets[0].data as (number | null)[]).every(v => v === null);
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
                                    {privacyRestricted || isEmpty ? (
                                        <NoDataOverlay
                                            message={privacyRestricted ? 'Datenschutz aktiv — keine Daten verfügbar' : 'Keine Daten verfügbar'}
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

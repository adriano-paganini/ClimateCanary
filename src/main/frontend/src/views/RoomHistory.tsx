import '../styles/App.css';
import 'primeicons/primeicons.css';
import React, { useCallback, useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Button } from 'primereact/button';
import { Chart } from 'primereact/chart';
import { Message } from 'primereact/message';
import { ProgressSpinner } from 'primereact/progressspinner';
import { format, subDays, subHours } from 'date-fns';

import globalAxios from 'axios';

import NavbarComponent from '../components/NavbarComponent';
import { useUser } from '../Contexts/AuthenticatedUserContext';
import { ThresholdService } from '../services/ThresholdService';
import { RoomService } from '../services/RoomService';
import {
    MeasurementDTOMetricEnum,
    RoomDTO,
    ThresholdDTO,
    ThresholdDTOThresholdTypeEnum,
    UserxRole,
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
    const { currentUser } = useUser();
    const numId     = roomId ? parseInt(roomId, 10) : NaN;

    const [timeRange,  setTimeRange]  = useState<TimeRange>('24h');
    const [room,       setRoom]       = useState<RoomDTO | null>(null);
    const [trendsMap,  setTrendsMap]  = useState<Record<string, TrendPoint[]>>({});
    const [thresholds, setThresholds] = useState<ThresholdDTO[]>([]);
    const [loading,    setLoading]    = useState(true);
    const [error,      setError]      = useState<string | null>(null);

    useEffect(() => {
        if (isNaN(numId)) return;
        RoomService.getById(numId).then(setRoom).catch(() => {});
        ThresholdService.getAll({ roomId: numId }).then(setThresholds).catch(() => {});
    }, [numId]);

    const loadMeasurements = useCallback(async () => {
        if (isNaN(numId)) return;
        setLoading(true);
        setError(null);
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
        } catch {
            setError('Failed to load measurements.');
        } finally {
            setLoading(false);
        }
    }, [numId, timeRange]);

    useEffect(() => { void loadMeasurements(); }, [loadMeasurements]);

    function buildChartData(metric: MeasurementDTOMetricEnum, _color: string) {
        const points = trendsMap[metric] ?? [];

        const labels = points.map(p => tickLabel(p.timestamp, timeRange));
        const values = points.map(p => p.value ?? null);

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const datasets: any[] = [{
            label:                metric,
            data:                 values,
            fill:                 false,
            borderColor:          '#3b82f6',
            backgroundColor:      'rgba(59, 130, 246, 0.08)',
            borderWidth:          1.5,
            tension:              0.3,
            pointRadius:          points.length > 200 ? 0 : 4,
            pointBackgroundColor: '#3b82f6',
            pointBorderColor:     '#3b82f6',
            spanGaps:             false,
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
                            currentUser?.roles?.has(UserxRole.DEPARTMENT_LEAD)
                                ? ROUTES.DEPARTMENT_DASHBOARD
                                : ROUTES.DASHBOARD,
                        )}
                    />
                    <h2 style={{ margin: 0, color: '#111827' }}>
                        {room?.name ? `${room.name} — History` : `Room ${roomId} — History`}
                    </h2>
                </div>

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

                {loading ? (
                    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '40vh' }}>
                        <ProgressSpinner />
                    </div>
                ) : (
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr', gap: '1.5rem' }}>
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
                                    {isEmpty ? (
                                        <div style={{
                                            height:          '220px',
                                            display:         'flex',
                                            alignItems:      'center',
                                            justifyContent:  'center',
                                            color:           '#9ca3af',
                                            backgroundColor: '#f9fafb',
                                            borderRadius:    '6px',
                                            fontSize:        '0.9rem',
                                        }}>
                                            No data for this period
                                        </div>
                                    ) : (
                                        <div style={{ height: '260px' }}>
                                            <Chart type="line" data={data} options={CHART_OPTIONS} plugins={[dropLinePlugin]} style={{ height: '100%' }} />
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

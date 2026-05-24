import { format } from 'date-fns';

import {
    MeasurementDTOMetricEnum,
    ThresholdDTO,
    ThresholdDTOThresholdTypeEnum,
} from '../generated-skeleton-api';

export interface TrendPoint {
    timestamp: string;
    value: number;
}

function tickLabel(ts: string): string {
    return format(new Date(ts), 'HH:mm');
}

export function buildRoomHistoryChartData(
    metric: MeasurementDTOMetricEnum,
    points: TrendPoint[],
    thresholds: ThresholdDTO[],
) {
    const activeThresholds = thresholds.filter(t => t.metric === metric && t.enabled !== false && t.boundValue !== undefined);
    const labels = points.length > 0
        ? points.map(p => tickLabel(p.timestamp))
        : activeThresholds.length > 0
            ? ['00:00', '06:00', '12:00', '18:00', '24:00']
            : [];
    const values = points.length > 0 ? points.map(p => p.value ?? null) : labels.map(() => null);

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

    for (const t of activeThresholds) {
        const isUpper = t.thresholdType === ThresholdDTOThresholdTypeEnum.UPPER;
        datasets.push({
            label:       isUpper ? 'Upper limit' : 'Lower limit',
            data:        labels.map(() => t.boundValue),
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

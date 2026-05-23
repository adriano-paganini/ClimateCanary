import globalAxios from "axios";
import { MeasurementDTOMetricEnum } from "../generated-skeleton-api";

export interface MetricSummaryDTO {
    latest?: number;
    avg?: number;
    min?: number;
    max?: number;
    count?: number;
}

export interface RoomSummaryDTO {
    roomId?: number;
    roomName?: string;
    generatedAt?: string;
    metrics?: Partial<Record<MeasurementDTOMetricEnum, MetricSummaryDTO>>;
}

export interface TrendPointDTO {
    timestamp?: string;
    value?: number;
}

export interface RoomTrendDTO {
    roomId?: number;
    metric?: MeasurementDTOMetricEnum;
    bucketSize?: string;
    granularityReduced?: boolean;
    points?: TrendPointDTO[];
}

export interface ViolationBreakdownDTO {
    label?: string;
    count?: number;
}

export interface RoomViolationSummaryDTO {
    roomId?: number;
    roomName?: string;
    total?: number;
    active?: number;
    resolved?: number;
    byMetric?: ViolationBreakdownDTO[];
}

const dateParams = (from?: string, to?: string) => ({
    ...(from ? { from } : {}),
    ...(to ? { to } : {}),
});

export const AnalyticsService = {
    getRoomSummary: (roomId: number, from?: string, to?: string): Promise<RoomSummaryDTO> =>
        globalAxios
            .get<RoomSummaryDTO>(`/api/analytics/rooms/${roomId}/summary`, { params: dateParams(from, to) })
            .then(r => r.data),

    getRoomTrend: (
        roomId: number,
        metric: MeasurementDTOMetricEnum,
        from?: string,
        to?: string,
    ): Promise<RoomTrendDTO> =>
        globalAxios
            .get<RoomTrendDTO>(`/api/analytics/rooms/${roomId}/trends`, {
                params: { metric, ...dateParams(from, to) },
            })
            .then(r => r.data),

    getRoomViolations: (roomId: number): Promise<RoomViolationSummaryDTO> =>
        globalAxios
            .get<RoomViolationSummaryDTO>(`/api/analytics/rooms/${roomId}/violations`)
            .then(r => r.data),
};

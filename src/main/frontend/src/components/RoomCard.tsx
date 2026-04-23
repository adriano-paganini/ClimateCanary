import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Card } from 'primereact/card';
import { Badge } from 'primereact/badge';
import 'primeicons/primeicons.css';

import {
    MeasurementDTO,
    MeasurementDTOMetricEnum,
    RoomDTO,
    ThresholdViolationDTO,
    ThresholdViolationDTOMetricEnum,
    ThresholdViolationDTOViolationStatusEnum,
} from '../generated-skeleton-api';
import { ROUTES } from '../utilities/routes.paths';

interface RoomCardProps {
    room: RoomDTO;
    measurements: MeasurementDTO[];
    violations: ThresholdViolationDTO[];
}

function latestMeasurement(measurements: MeasurementDTO[], metric: MeasurementDTOMetricEnum): MeasurementDTO | undefined {
    return measurements
        .filter(m => m.metric === metric)
        .sort((a, b) => new Date(b.timestamp ?? 0).getTime() - new Date(a.timestamp ?? 0).getTime())[0];
}

function airQualityColor(
    measurements: MeasurementDTO[],
    violations: ThresholdViolationDTO[],
): string {
    const hasGasData = measurements.some(m => m.metric === MeasurementDTOMetricEnum.GAS);
    if (!hasGasData) return '#9ca3af';
    const gasViolation = violations.some(
        v =>
            v.violationStatus === ThresholdViolationDTOViolationStatusEnum.ACTIVE &&
            v.metric === (ThresholdViolationDTOMetricEnum.GAS as string),
    );
    return gasViolation ? '#ef4444' : '#22c55e';
}

const RoomCard: React.FC<RoomCardProps> = ({ room, measurements, violations }) => {
    const navigate = useNavigate();

    const noData = measurements.length === 0;
    const activeViolations = violations.filter(
        v => v.violationStatus === ThresholdViolationDTOViolationStatusEnum.ACTIVE,
    );
    const latestTemp = latestMeasurement(measurements, MeasurementDTOMetricEnum.TEMPERATURE);
    const latestGas = latestMeasurement(measurements, MeasurementDTOMetricEnum.GAS);
    const qualityColor = airQualityColor(measurements, violations);

    const handleClick = () => {
        if (room.id !== undefined) {
            navigate(ROUTES.DASHBOARD_HISTORY.replace(':roomId', String(room.id)));
        }
    };

    return (
        <Card
            onClick={handleClick}
            style={{ position: 'relative', cursor: 'pointer', minWidth: '200px' }}
            className="shadow-2 hover:shadow-4"
        >
            {noData && (
                <div style={{
                    position: 'absolute', inset: 0,
                    backgroundColor: 'rgba(156,163,175,0.65)',
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    borderRadius: '6px', zIndex: 10,
                }}>
                    <span style={{ fontWeight: 600, color: '#374151' }}>Keine Daten</span>
                </div>
            )}

            {/* Header row: name + warning badge */}
            <div className="flex justify-content-between align-items-center mb-3">
                <span style={{ fontWeight: 700, fontSize: '1rem' }}>{room.name ?? '—'}</span>
                {activeViolations.length > 0 && (
                    <span className="flex align-items-center gap-1" style={{ color: '#ef4444' }}>
                        <i className="pi pi-exclamation-triangle mr-1" />
                        <Badge value={activeViolations.length} severity="danger" />
                    </span>
                )}
            </div>

            {/* Temperature */}
            <div className="flex align-items-center gap-2 mb-2">
                <span style={{ color: '#6b7280', fontSize: '0.875rem' }}>Temperatur:</span>
                <span style={{ fontWeight: 600 }}>
                    {latestTemp?.measurement !== undefined ? `${latestTemp.measurement} °C` : '—'}
                </span>
            </div>

            {/* Air quality indicator */}
            <div className="flex align-items-center gap-2 mb-3">
                <span style={{ color: '#6b7280', fontSize: '0.875rem' }}>Luftqualität:</span>
                <div style={{
                    width: '18px', height: '18px', borderRadius: '3px',
                    backgroundColor: qualityColor, border: '1px solid rgba(0,0,0,0.12)',
                    flexShrink: 0,
                }} />
                {latestGas?.measurement !== undefined && (
                    <span style={{ fontSize: '0.8rem', color: '#6b7280' }}>
                        {latestGas.measurement}
                    </span>
                )}
            </div>

            {/* Navigation arrow */}
            <div className="flex justify-content-end">
                <i className="pi pi-arrow-right" style={{ color: '#9ca3af' }} />
            </div>
        </Card>
    );
};

export default RoomCard;

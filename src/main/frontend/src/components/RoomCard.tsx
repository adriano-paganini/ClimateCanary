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
    const hasIaqData = measurements.some(m => m.metric === MeasurementDTOMetricEnum.IAQ);
    if (!hasIaqData) return '#9ca3af';
    const iaqViolation = violations.some(
        v =>
            v.violationStatus === ThresholdViolationDTOViolationStatusEnum.ACTIVE &&
            v.metric === (ThresholdViolationDTOMetricEnum.IAQ as string),
    );
    return iaqViolation ? '#ef4444' : '#22c55e';
}

const RoomCard: React.FC<RoomCardProps> = ({ room, measurements, violations }) => {
    const navigate = useNavigate();

    const noData = measurements.length === 0;
    const activeViolations = violations.filter(
        v => v.violationStatus === ThresholdViolationDTOViolationStatusEnum.ACTIVE,
    );
    const latestTemp = latestMeasurement(measurements, MeasurementDTOMetricEnum.TEMPERATURE);
    const latestIaq = latestMeasurement(measurements, MeasurementDTOMetricEnum.IAQ);
    const qualityColor = airQualityColor(measurements, violations);

    const handleClick = () => {
        if (room.id !== undefined) {
            navigate(ROUTES.DASHBOARD_HISTORY.replace(':roomId', String(room.id)));
        }
    };

    return (
        <Card
            onClick={handleClick}
            style={{
                position: 'relative',
                cursor: 'pointer',
                minWidth: '260px',
                border: '1px solid #d1d5db',
                borderRadius: '10px',
            }}
            className="hover:shadow-4"
        >
            {noData && (
                <div style={{
                    position: 'absolute', inset: 0,
                    backgroundColor: 'rgba(156,163,175,0.65)',
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    borderRadius: '10px', zIndex: 10,
                }}>
                    <span style={{ fontWeight: 600, color: '#374151', fontSize: '0.95rem' }}>No Data</span>
                </div>
            )}

            {/* Header: room name + violation badge */}
            <div style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                marginBottom: '1rem',
                paddingBottom: '0.75rem',
                borderBottom: '1px solid #f3f4f6',
            }}>
                <span style={{ fontWeight: 700, fontSize: '1.05rem', color: '#111827' }}>
                    {room.name ?? '—'}
                </span>
                {activeViolations.length > 0 && (
                    <span style={{ display: 'flex', alignItems: 'center', gap: '6px', color: '#ef4444' }}>
                        <i className="pi pi-exclamation-triangle" style={{ fontSize: '1.3rem' }} />
                        <Badge value={activeViolations.length} severity="danger" size="large" />
                    </span>
                )}
            </div>

            {/* Metrics */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.6rem', marginBottom: '1rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span style={{ color: '#6b7280', fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: '6px' }}>
                        <i className="pi pi-sun mr-2" />
                        Temperature
                    </span>
                    <span style={{ fontWeight: 600, color: '#111827' }}>
                        {latestTemp?.measurement !== undefined ? `${latestTemp.measurement} °C` : '—'}
                    </span>
                </div>

                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span style={{ color: '#6b7280', fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: '6px' }}>
                        <i className="pi pi-cloud mr-2" />
                        Air Quality
                    </span>
                    <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                        <div style={{
                            width: '14px', height: '14px', borderRadius: '3px',
                            backgroundColor: qualityColor,
                            border: '1px solid rgba(0,0,0,0.15)',
                            flexShrink: 0,
                        }} />
                        {latestIaq?.measurement !== undefined && (
                            <span style={{ fontWeight: 600, color: '#111827', fontSize: '0.9rem' }}>
                                {latestIaq.measurement.toFixed(1)}
                            </span>
                        )}
                    </span>
                </div>
            </div>

            {/* Footer: link hint */}
            <div style={{ display: 'flex', justifyContent: 'flex-end', color: '#9ca3af', fontSize: '0.8rem', gap: '0.5rem' }}>
                <span>View history</span>
                <i className="pi pi-arrow-circle-right" style={{ fontSize: '0.8rem', display: 'flex', alignItems: 'center' }} />
            </div>
        </Card>
    );
};

export default RoomCard;

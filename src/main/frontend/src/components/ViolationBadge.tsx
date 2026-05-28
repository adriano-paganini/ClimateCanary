import React from 'react';
import { Badge } from 'primereact/badge';
import { ThresholdViolationDTO, ThresholdViolationDTOViolationStatusEnum } from '../generated-skeleton-api';

interface ViolationBadgeProps {
    violations: ThresholdViolationDTO[];
}

const ViolationBadge: React.FC<ViolationBadgeProps> = ({ violations }) => {
    const activeCount = violations.filter(
        v => v.violationStatus === ThresholdViolationDTOViolationStatusEnum.ACTIVE,
    ).length;

    if (activeCount === 0) return null;

    return (
        <span style={{ display: 'flex', alignItems: 'center', gap: '6px', color: '#ef4444' }}>
            <i className="pi pi-exclamation-triangle" style={{ fontSize: '1.1rem' }} />
            <Badge value={activeCount} severity="danger" />
        </span>
    );
};

export default ViolationBadge;

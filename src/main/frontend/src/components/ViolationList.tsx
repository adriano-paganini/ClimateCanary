import React from 'react';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { Tag } from 'primereact/tag';
import {
    ThresholdViolationDTO,
    ThresholdViolationDTOMetricEnum,
    ThresholdViolationDTOViolationStatusEnum,
} from '../generated-skeleton-api';

interface ViolationListProps {
    violations: ThresholdViolationDTO[];
}

const METRIC_LABEL: Record<string, string> = {
    [ThresholdViolationDTOMetricEnum.TEMPERATURE]: 'Temperatur',
    [ThresholdViolationDTOMetricEnum.HUMIDITY]: 'Luftfeuchtigkeit',
    [ThresholdViolationDTOMetricEnum.PRESSURE]: 'Luftdruck',
    [ThresholdViolationDTOMetricEnum.IAQ]: 'Luftqualität',
};

const METRIC_UNIT: Record<string, string> = {
    [ThresholdViolationDTOMetricEnum.TEMPERATURE]: '°C',
    [ThresholdViolationDTOMetricEnum.HUMIDITY]: '%',
    [ThresholdViolationDTOMetricEnum.PRESSURE]: 'hPa',
    [ThresholdViolationDTOMetricEnum.IAQ]: '',
};

type TagSeverity = 'success' | 'info' | 'warning' | 'danger' | 'secondary' | 'contrast';

const STATUS_SEVERITY: Record<string, TagSeverity> = {
    [ThresholdViolationDTOViolationStatusEnum.ACTIVE]: 'danger',
    [ThresholdViolationDTOViolationStatusEnum.RESOLVED]: 'success',
    [ThresholdViolationDTOViolationStatusEnum.DISABLED]: 'secondary',
};

const STATUS_LABEL: Record<string, string> = {
    [ThresholdViolationDTOViolationStatusEnum.ACTIVE]: 'Aktiv',
    [ThresholdViolationDTOViolationStatusEnum.RESOLVED]: 'Behoben',
    [ThresholdViolationDTOViolationStatusEnum.DISABLED]: 'Deaktiviert',
};

function formatDateTime(iso: string | undefined): string {
    if (!iso) return '—';
    return new Date(iso).toLocaleString('de-AT', {
        day: '2-digit', month: '2-digit', year: 'numeric',
        hour: '2-digit', minute: '2-digit',
    });
}

const metricTemplate = (row: ThresholdViolationDTO) => {
    const metric = row.metric ?? '';
    const label = METRIC_LABEL[metric] ?? metric;
    const unit = METRIC_UNIT[metric] ?? '';
    const value = row.value !== undefined ? row.value.toFixed(1) : '—';
    return <span>{label}: {value}{unit ? ' ' + unit : ''}</span>;
};

const zeitraumTemplate = (row: ThresholdViolationDTO) => {
    const start = formatDateTime(row.startTime);
    const end = row.endTime ? formatDateTime(row.endTime) : 'Noch aktiv';
    return <span style={{ fontSize: '0.875rem', color: '#374151' }}>{start} – {end}</span>;
};

const statusTemplate = (row: ThresholdViolationDTO) => {
    const status = row.violationStatus ?? '';
    return (
        <Tag
            value={STATUS_LABEL[status] ?? status}
            severity={STATUS_SEVERITY[status] ?? 'info'}
        />
    );
};

const ViolationList: React.FC<ViolationListProps> = ({ violations }) => (
    <DataTable
        value={violations}
        emptyMessage="Keine Warnungen vorhanden"
        sortField="startTime"
        sortOrder={-1}
        paginator={violations.length > 10}
        rows={10}
        style={{ fontSize: '0.9rem' }}
    >
        <Column header="Metrik / Messwert" body={metricTemplate} sortable sortField="metric" />
        <Column header="Zeitraum" body={zeitraumTemplate} sortable sortField="startTime" />
        <Column header="Status" body={statusTemplate} sortable sortField="violationStatus" style={{ width: '130px' }} />
    </DataTable>
);

export default ViolationList;

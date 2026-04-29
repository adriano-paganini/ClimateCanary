import React, { useEffect, useState } from 'react';
import { Button } from 'primereact/button';
import { Calendar } from 'primereact/calendar';
import { Dropdown } from 'primereact/dropdown';
import { Message } from 'primereact/message';
import { ProgressSpinner } from 'primereact/progressspinner';
import { Tag } from 'primereact/tag';
import 'primeicons/primeicons.css';

import NavbarComponent from '../components/NavbarComponent';
import { UserService } from '../services/UserService';
import { AbsenceService } from '../services/AbsenceService';
import {
    AbsenceCreateDTOAbsenceTypeEnum,
    AbsenceDTO,
    AbsenceDTOAbsenceStatusEnum,
    AbsenceUpdateDTOAbsenceStatusEnum,
} from '../generated-skeleton-api';

const ABSENCE_TYPE_OPTIONS = [
    { label: 'Holiday', value: AbsenceCreateDTOAbsenceTypeEnum.HOLIDAY },
    { label: 'Sick Leave', value: AbsenceCreateDTOAbsenceTypeEnum.SICKNESS },
    { label: 'Parental Leave', value: AbsenceCreateDTOAbsenceTypeEnum.PARENTAL_LEAVE },
    { label: 'Other', value: AbsenceCreateDTOAbsenceTypeEnum.OTHER },
];

function toISOLocal(date: Date): string {
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:00`;
}

function formatDisplayRange(start?: string, end?: string): string {
    if (!start) return '—';
    const fmt = (s: string) => {
        const d = new Date(s);
        const hasTime = d.getHours() !== 0 || d.getMinutes() !== 0;
        const date = d.toLocaleDateString('en-GB', { day: '2-digit', month: '2-digit', year: 'numeric' });
        const time = hasTime ? ` ${d.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' })}` : '';
        return `${date}${time}`;
    };
    return end ? `${fmt(start)} – ${fmt(end)}` : fmt(start);
}

function absenceTypeLabel(type?: string): string {
    switch (type) {
        case 'HOLIDAY': return 'Holiday';
        case 'SICKNESS': return 'Sick Leave';
        case 'PARENTAL_LEAVE': return 'Parental Leave';
        case 'OTHER': return 'Other';
        default: return type ?? '—';
    }
}

function statusSeverity(status?: string): 'success' | 'info' | 'warning' | 'danger' | null | undefined {
    switch (status) {
        case 'APPROVED': return 'success';
        case 'PLANNED': return 'info';
        case 'REJECTED': return 'danger';
        case 'CANCELLED': return 'warning';
        default: return null;
    }
}

function statusLabel(status?: string): string {
    switch (status) {
        case 'APPROVED': return 'Approved';
        case 'PLANNED': return 'Planned';
        case 'REJECTED': return 'Rejected';
        case 'CANCELLED': return 'Cancelled';
        default: return status ?? '—';
    }
}

const cardStyle: React.CSSProperties = {
    background: '#fff',
    border: '1px solid #e5e7eb',
    borderRadius: '8px',
    padding: '1.5rem',
    marginBottom: '1.5rem',
};

const labelStyle: React.CSSProperties = {
    display: 'block',
    fontSize: '0.85rem',
    fontWeight: 600,
    color: '#374151',
    marginBottom: '0.35rem',
};

const AbsenceView: React.FC = () => {
    const [absences, setAbsences] = useState<AbsenceDTO[]>([]);
    const [loading, setLoading] = useState(true);
    const [loadError, setLoadError] = useState<string | null>(null);
    const [userId, setUserId] = useState<number | null>(null);

    const [startDate, setStartDate] = useState<Date | null>(null);
    const [endDate, setEndDate] = useState<Date | null>(null);
    const [absenceType, setAbsenceType] = useState<string | null>(null);

    const [submitting, setSubmitting] = useState(false);
    const [formError, setFormError] = useState<string | null>(null);
    const [successMsg, setSuccessMsg] = useState<string | null>(null);

    const [cancelling, setCancelling] = useState<number | null>(null);

    useEffect(() => {
        const load = async () => {
            try {
                const [user, myAbsences] = await Promise.all([
                    UserService.getCurrentUser(),
                    UserService.getCurrentUserAbsences(),
                ]);
                setUserId(user.id ?? null);
                setAbsences(sortAbsences(myAbsences));
            } catch (err) {
                setLoadError(err instanceof Error ? err.message : String(err));
            } finally {
                setLoading(false);
            }
        };
        void load();
    }, []);

    const sortAbsences = (list: AbsenceDTO[]) =>
        [...list].sort((a, b) => {
            const da = a.startDate ? new Date(a.startDate).getTime() : 0;
            const db = b.startDate ? new Date(b.startDate).getTime() : 0;
            return db - da;
        });

    const handleSubmit = async () => {
        setFormError(null);
        setSuccessMsg(null);

        if (!startDate || !endDate) {
            setFormError('Please select a start and end date.');
            return;
        }
        if (!absenceType) {
            setFormError('Please select a reason.');
            return;
        }
        if (startDate >= endDate) {
            setFormError('The start date must be before the end date.');
            return;
        }
        if (!userId) {
            setFormError('User ID not available.');
            return;
        }

        setSubmitting(true);
        try {
            const created = await AbsenceService.create({
                startDate: toISOLocal(startDate),
                endDate: toISOLocal(endDate),
                absenceType: absenceType as AbsenceCreateDTOAbsenceTypeEnum,
                userxId: userId,
            });
            setAbsences(prev => sortAbsences([created, ...prev]));
            setSuccessMsg('Absence successfully recorded.');
            setStartDate(null);
            setEndDate(null);
            setAbsenceType(null);
        } catch (err) {
            setFormError(err instanceof Error ? err.message : String(err));
        } finally {
            setSubmitting(false);
        }
    };

    const handleCancel = async (absence: AbsenceDTO) => {
        if (!absence.id) return;
        setCancelling(absence.id);
        try {
            const updated = await AbsenceService.update(absence.id, {
                absenceStatus: AbsenceUpdateDTOAbsenceStatusEnum.CANCELLED,
            });
            setAbsences(prev => sortAbsences(prev.map(a => (a.id === updated.id ? updated : a))));
        } catch (err) {
            setFormError(err instanceof Error ? err.message : String(err));
        } finally {
            setCancelling(null);
        }
    };

    const canCancel = (a: AbsenceDTO) =>
        a.absenceStatus === AbsenceDTOAbsenceStatusEnum.PLANNED ||
        a.absenceStatus === AbsenceDTOAbsenceStatusEnum.APPROVED;

    if (loading) {
        return (
            <div>
                <NavbarComponent />
                <div className="flex justify-content-center align-items-center" style={{ minHeight: '60vh' }}>
                    <ProgressSpinner />
                </div>
            </div>
        );
    }

    if (loadError) {
        return (
            <div>
                <NavbarComponent />
                <div className="m-4">
                    <Message severity="error" text={`Failed to load absences: ${loadError}`} />
                </div>
            </div>
        );
    }

    return (
        <div>
            <NavbarComponent />
            <div style={{ padding: '1.5rem 2rem' }}>
                <h2 style={{ margin: '0 0 1.5rem', color: '#111827' }}>Absences</h2>

                {/* Create form */}
                <div style={cardStyle}>
                    <h3 style={{ margin: '0 0 1.25rem', color: '#374151', fontSize: '1rem' }}>
                        <i className="pi pi-plus-circle" style={{ marginRight: '0.5rem', color: '#0369a1' }} />
                        Add Absence
                    </h3>

                    {successMsg && (
                        <div style={{ marginBottom: '1rem' }}>
                            <Message severity="success" text={successMsg} />
                        </div>
                    )}
                    {formError && (
                        <div style={{ marginBottom: '1rem' }}>
                            <Message severity="error" text={formError} />
                        </div>
                    )}

                    <div style={{ display: 'flex', gap: '1.5rem', flexWrap: 'wrap', marginBottom: '1.25rem' }}>
                        <div style={{ flex: 1, minWidth: '220px' }}>
                            <label style={labelStyle}>From</label>
                            <Calendar
                                value={startDate}
                                onChange={e => setStartDate(e.value as Date | null)}
                                showTime
                                hourFormat="24"
                                dateFormat="dd.mm.yy"
                                placeholder="dd.mm.yyyy hh:mm"
                                style={{ width: '100%' }}
                            />
                        </div>
                        <div style={{ flex: 1, minWidth: '220px' }}>
                            <label style={labelStyle}>To</label>
                            <Calendar
                                value={endDate}
                                onChange={e => setEndDate(e.value as Date | null)}
                                showTime
                                hourFormat="24"
                                dateFormat="dd.mm.yy"
                                placeholder="dd.mm.yyyy hh:mm"
                                minDate={startDate ?? undefined}
                                style={{ width: '100%' }}
                            />
                        </div>
                        <div style={{ flex: 1, minWidth: '180px' }}>
                            <label style={labelStyle}>Reason</label>
                            <Dropdown
                                options={ABSENCE_TYPE_OPTIONS}
                                value={absenceType}
                                onChange={e => setAbsenceType(e.value as string)}
                                placeholder="Select..."
                                style={{ width: '100%' }}
                            />
                        </div>
                    </div>

                    <Button
                        label="Submit"
                        icon="pi pi-send"
                        loading={submitting}
                        onClick={() => void handleSubmit()}
                        style={{ background: '#111827', border: 'none' }}
                    />
                </div>

                {/* Past absences */}
                <div style={cardStyle}>
                    <h3 style={{ margin: '0 0 1.25rem', color: '#374151', fontSize: '1rem' }}>
                        <i className="pi pi-calendar" style={{ marginRight: '0.5rem', color: '#0369a1' }} />
                        Past Absences
                    </h3>

                    {absences.length === 0 ? (
                        <p style={{ color: '#6b7280', margin: 0 }}>No absences recorded yet.</p>
                    ) : (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                            {absences.map(a => (
                                <div
                                    key={a.id}
                                    style={{
                                        display: 'flex',
                                        alignItems: 'center',
                                        justifyContent: 'space-between',
                                        padding: '0.75rem 1rem',
                                        background: '#f9fafb',
                                        borderRadius: '6px',
                                        border: '1px solid #e5e7eb',
                                        flexWrap: 'wrap',
                                        gap: '0.5rem',
                                    }}
                                >
                                    <span style={{ color: '#374151', fontWeight: 500, minWidth: '260px' }}>
                                        {formatDisplayRange(a.startDate, a.endDate)}
                                    </span>
                                    <span style={{ color: '#6b7280', minWidth: '100px' }}>
                                        {absenceTypeLabel(a.absenceType)}
                                    </span>
                                    <Tag
                                        value={statusLabel(a.absenceStatus)}
                                        severity={statusSeverity(a.absenceStatus) ?? undefined}
                                        style={{ minWidth: '90px', textAlign: 'center' }}
                                    />
                                    {canCancel(a) && (
                                        <Button
                                            label="Cancel"
                                            icon="pi pi-times"
                                            className="p-button-outlined p-button-danger p-button-sm"
                                            loading={cancelling === a.id}
                                            onClick={() => void handleCancel(a)}
                                        />
                                    )}
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default AbsenceView;

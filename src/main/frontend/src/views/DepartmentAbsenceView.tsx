import React, { useEffect, useRef, useState } from 'react';
import { ProgressSpinner } from 'primereact/progressspinner';
import { Message } from 'primereact/message';
import { Button } from 'primereact/button';
import { Tag } from 'primereact/tag';
import { Dropdown } from 'primereact/dropdown';
import { Calendar } from 'primereact/calendar';
import { Toast } from 'primereact/toast';
import { Badge } from 'primereact/badge';
import 'primeicons/primeicons.css';

import NavbarComponent from '../components/NavbarComponent';
import { FooterComponent } from '../components/FooterComponent';
import { useUser } from '../Contexts/AuthenticatedUserContext';
import { DepartmentService } from '../services/DepartmentService';
import {
    AbsenceControllerApi,
    AbsenceDTO,
    AbsenceDTOAbsenceStatusEnum,
    AbsenceUpdateDTOAbsenceStatusEnum,
    AdminControllerApi,
    DepartmentDTO,
    UserxDTO,
} from '../generated-skeleton-api';

const STATUS_SEVERITY: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
    APPROVED: 'success',
    REJECTED: 'danger',
    PLANNED: 'info',
    CANCELLED: 'warning',
};

const STATUS_ICON: Record<string, string> = {
    APPROVED: 'pi-check-circle',
    REJECTED: 'pi-times-circle',
    PLANNED: 'pi-clock',
    CANCELLED: 'pi-ban',
};

const DepartmentAbsenceView: React.FC = () => {
    const { fullUser } = useUser();
    const toast = useRef<Toast>(null);

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [department, setDepartment] = useState<DepartmentDTO | null>(null);
    const [absences, setAbsences] = useState<AbsenceDTO[]>([]);
    const [userMap, setUserMap] = useState<Record<number, UserxDTO>>({});

    const [statusFilter, setStatusFilter] = useState<AbsenceDTOAbsenceStatusEnum | null>(null);
    const [dateRange, setDateRange] = useState<[Date | null, Date | null]>([null, null]);

    useEffect(() => {
        const load = async () => {
            try {
                const userId = fullUser?.id;
                if (!userId) {
                    setError('Could not determine current user.');
                    return;
                }

                const allDepts = await DepartmentService.getAll();
                const myDept = allDepts.find(d => d.departmentLeadId === userId);
                if (!myDept?.id) {
                    setError('No department found for this department lead.');
                    return;
                }
                setDepartment(myDept);

                const absenceApi = new AbsenceControllerApi();
                const absenceData = await absenceApi.getAll8({ departmentId: myDept.id }).then(r => r.data);
                setAbsences(absenceData);

                // User names are loaded via admin API — may not be accessible for all roles.
                // If 403, the table falls back to showing userxId.
                try {
                    const adminApi = new AdminControllerApi();
                    const userData = await adminApi.getAllUsers().then(r => r.data);
                    const map: Record<number, UserxDTO> = {};
                    userData.forEach(u => { if (u.id !== undefined) map[u.id] = u; });
                    setUserMap(map);
                } catch {
                    // insufficient permissions — userxId shown as fallback
                }
            } catch (err: unknown) {
                const msg = err instanceof Error ? err.message : String(err);
                console.error('DepartmentAbsenceView load error:', err);
                setError(`Failed to load data: ${msg}`);
            } finally {
                setLoading(false);
            }
        };
        void load();
    }, [fullUser?.id]);

    const updateStatus = async (absence: AbsenceDTO, newStatus: AbsenceUpdateDTOAbsenceStatusEnum) => {
        if (!absence.id) return;
        try {
            const absenceApi = new AbsenceControllerApi();
            const updated = await absenceApi.update10({
                id: absence.id,
                absenceUpdateDTO: { absenceStatus: newStatus },
            }).then(r => r.data);
            setAbsences(prev => prev.map(a => a.id === updated.id ? updated : a));
            toast.current?.show({
                severity: newStatus === AbsenceUpdateDTOAbsenceStatusEnum.APPROVED ? 'success' : 'warn',
                summary: newStatus === AbsenceUpdateDTOAbsenceStatusEnum.APPROVED ? 'Approved' : 'Rejected',
                detail: `Absence has been ${newStatus.toLowerCase()}.`,
                life: 3000,
            });
        } catch (err) {
            console.error('Error updating absence:', err);
            toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to update absence.', life: 3000 });
        }
    };

    const filteredAbsences = absences.filter(a => {
        if (statusFilter && a.absenceStatus !== statusFilter) return false;
        if (dateRange[0] && a.endDate && new Date(a.endDate) < dateRange[0]) return false;
        if (dateRange[1] && a.startDate && new Date(a.startDate) > dateRange[1]) return false;
        return true;
    });

    const getEmployeeName = (row: AbsenceDTO) => {
        if (row.userxId === undefined) return '—';
        const user = userMap[row.userxId];
        if (!user) return `User ${row.userxId}`;
        const name = `${user.firstName ?? ''} ${user.lastName ?? ''}`.trim();
        return name || user.username || `User ${row.userxId}`;
    };

    const getPeriodString = (row: AbsenceDTO) => {
        const from = row.startDate ? new Date(row.startDate).toLocaleDateString('en-GB', { day: '2-digit', month: '2-digit', year: 'numeric' }) : '?';
        const to = row.endDate ? new Date(row.endDate).toLocaleDateString('en-GB', { day: '2-digit', month: '2-digit', year: 'numeric' }) : '?';
        return `${from} – ${to}`;
    };

    const getAbsenceTypeLabel = (type?: string): string => {
        switch (type) {
            case 'HOLIDAY': return 'Holiday';
            case 'SICKNESS': return 'Sick Leave';
            case 'PARENTAL_LEAVE': return 'Parental Leave';
            case 'OTHER': return 'Other';
            default: return type ?? '—';
        }
    };

    const statusOptions = Object.values(AbsenceDTOAbsenceStatusEnum).map(s => ({ label: s, value: s }));

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

    if (error) {
        return (
            <div>
                <NavbarComponent />
                <div className="m-4">
                    <Message severity="error" text={error} />
                </div>
            </div>
        );
    }

    return (
        <div>
            <NavbarComponent />
            <Toast ref={toast} />

            <div style={{ padding: '1.5rem 2rem', maxWidth: '1400px', margin: '0 auto' }}>
                {/* Header */}
                <div style={{ marginBottom: '2rem' }}>
                    <h1 style={{ margin: '0 0 0.5rem', color: '#111827', fontSize: '2rem', fontWeight: 700 }}>
                        Team Absences
                    </h1>
                    {department && (
                        <p style={{ margin: 0, color: '#6b7280', fontSize: '0.95rem', display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                            <i className="pi pi-sitemap" />
                            {department.name}
                        </p>
                    )}
                </div>

                {/* Filters */}
                <div style={{
                    display: 'grid',
                    gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
                    gap: '1rem',
                    marginBottom: '2rem',
                    padding: '1.5rem',
                    backgroundColor: '#f9fafb',
                    borderRadius: '10px',
                    border: '1px solid #e5e7eb',
                }}>
                    <div>
                        <label style={{
                            display: 'block',
                            fontSize: '0.875rem',
                            fontWeight: 600,
                            color: '#374151',
                            marginBottom: '0.5rem',
                        }}>
                            Status
                        </label>
                        <Dropdown
                            value={statusFilter}
                            onChange={e => setStatusFilter(e.value)}
                            options={statusOptions}
                            placeholder="All statuses"
                            showClear
                            style={{ width: '100%' }}
                        />
                    </div>
                    <div>
                        <label style={{
                            display: 'block',
                            fontSize: '0.875rem',
                            fontWeight: 600,
                            color: '#374151',
                            marginBottom: '0.5rem',
                        }}>
                            Date Range
                        </label>
                        <Calendar
                            value={dateRange}
                            onChange={e => setDateRange((e.value as [Date | null, Date | null]) ?? [null, null])}
                            selectionMode="range"
                            readOnlyInput
                            placeholder="Filter by period"
                            showButtonBar
                            style={{ width: '100%' }}
                        />
                    </div>
                </div>

                {/* Stats Bar */}
                {filteredAbsences.length > 0 && (
                    <div style={{
                        display: 'flex',
                        gap: '1rem',
                        marginBottom: '1.5rem',
                        flexWrap: 'wrap',
                    }}>
                        <div style={{
                            padding: '0.75rem 1rem',
                            backgroundColor: '#f0f9ff',
                            border: '1px solid #bfdbfe',
                            borderRadius: '8px',
                            fontSize: '0.875rem',
                            color: '#1e40af',
                        }}>
                            <i className="pi pi-list" style={{ marginRight: '0.5rem' }} />
                            {filteredAbsences.length} absence{filteredAbsences.length !== 1 ? 's' : ''}
                        </div>
                        <div style={{
                            padding: '0.75rem 1rem',
                            backgroundColor: '#fef3c7',
                            border: '1px solid #fcd34d',
                            borderRadius: '8px',
                            fontSize: '0.875rem',
                            color: '#92400e',
                        }}>
                            <i className="pi pi-clock" style={{ marginRight: '0.5rem' }} />
                            {filteredAbsences.filter(a => a.absenceStatus === AbsenceDTOAbsenceStatusEnum.PLANNED).length} pending approval
                        </div>
                    </div>
                )}

                {/* Absences Grid */}
                {filteredAbsences.length === 0 ? (
                    <div style={{
                        padding: '3rem',
                        textAlign: 'center',
                        backgroundColor: '#f9fafb',
                        borderRadius: '10px',
                        border: '1px solid #e5e7eb',
                    }}>
                        <i className="pi pi-inbox" style={{ fontSize: '2rem', color: '#d1d5db', marginBottom: '1rem', display: 'block' }} />
                        <p style={{ color: '#6b7280', margin: 0 }}>No absences found for the selected criteria.</p>
                    </div>
                ) : (
                    <div style={{
                        display: 'grid',
                        gridTemplateColumns: 'repeat(auto-fill, minmax(340px, 1fr))',
                        gap: '1.5rem',
                    }}>
                        {filteredAbsences.map((absence) => (
                            <div
                                key={absence.id}
                                style={{
                                    backgroundColor: '#fff',
                                    border: '1px solid #e5e7eb',
                                    borderRadius: '10px',
                                    padding: '1.5rem',
                                    boxShadow: '0 1px 3px rgba(0, 0, 0, 0.05)',
                                    transition: 'all 0.2s ease',
                                    cursor: 'default',
                                }}
                                onMouseEnter={(e) => {
                                    (e.currentTarget as HTMLElement).style.boxShadow = '0 4px 12px rgba(0, 0, 0, 0.1)';
                                    (e.currentTarget as HTMLElement).style.borderColor = '#d1d5db';
                                }}
                                onMouseLeave={(e) => {
                                    (e.currentTarget as HTMLElement).style.boxShadow = '0 1px 3px rgba(0, 0, 0, 0.05)';
                                    (e.currentTarget as HTMLElement).style.borderColor = '#e5e7eb';
                                }}
                            >
                                {/* Top Section: Employee & Status */}
                                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1rem', gap: '1rem' }}>
                                    <div style={{ flex: 1 }}>
                                        <h3 style={{
                                            margin: '0 0 0.25rem',
                                            color: '#111827',
                                            fontSize: '1.1rem',
                                            fontWeight: 600,
                                        }}>
                                            {getEmployeeName(absence)}
                                        </h3>
                                        <p style={{ margin: 0, color: '#6b7280', fontSize: '0.85rem' }}>
                                            {getAbsenceTypeLabel(absence.absenceType)}
                                        </p>
                                    </div>
                                    <div>
                                        {absence.absenceStatus && (
                                            <Tag
                                                value={absence.absenceStatus}
                                                severity={STATUS_SEVERITY[absence.absenceStatus] ?? 'info'}
                                                icon={`pi ${STATUS_ICON[absence.absenceStatus] || 'pi-info-circle'}`}
                                                style={{ fontSize: '0.8rem' }}
                                            />
                                        )}
                                    </div>
                                </div>

                                {/* Divider */}
                                <div style={{ height: '1px', backgroundColor: '#f3f4f6', margin: '1rem 0' }} />

                                {/* Period Section */}
                                <div style={{ marginBottom: '1rem' }}>
                                    <p style={{
                                        margin: '0 0 0.5rem',
                                        color: '#6b7280',
                                        fontSize: '0.8rem',
                                        fontWeight: 500,
                                        textTransform: 'uppercase',
                                        letterSpacing: '0.5px',
                                    }}>
                                        Period
                                    </p>
                                    <p style={{
                                        margin: 0,
                                        color: '#111827',
                                        fontSize: '0.95rem',
                                        fontWeight: 500,
                                    }}>
                                        <i className="pi pi-calendar" style={{ marginRight: '0.5rem', color: '#9ca3af', fontSize: '0.85rem' }} />
                                        {getPeriodString(absence)}
                                    </p>
                                </div>

                                {/* Actions */}
                                {absence.absenceStatus === AbsenceDTOAbsenceStatusEnum.PLANNED && (
                                    <div style={{
                                        display: 'grid',
                                        gridTemplateColumns: '1fr 1fr',
                                        gap: '0.75rem',
                                        marginTop: '1.5rem',
                                    }}>
                                        <Button
                                            label="Approve"
                                            icon="pi pi-check"
                                            severity="success"
                                            size="small"
                                            onClick={() => updateStatus(absence, AbsenceUpdateDTOAbsenceStatusEnum.APPROVED)}
                                            style={{ width: '100%' }}
                                        />
                                        <Button
                                            label="Reject"
                                            icon="pi pi-times"
                                            severity="danger"
                                            size="small"
                                            onClick={() => updateStatus(absence, AbsenceUpdateDTOAbsenceStatusEnum.REJECTED)}
                                            style={{ width: '100%' }}
                                        />
                                    </div>
                                )}
                            </div>
                        ))}
                    </div>
                )}
            </div>

            <FooterComponent />
        </div>
    );
};

export default DepartmentAbsenceView;

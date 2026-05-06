import React, { useEffect, useRef, useState } from 'react';
import { ProgressSpinner } from 'primereact/progressspinner';
import { Message } from 'primereact/message';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { Button } from 'primereact/button';
import { Tag } from 'primereact/tag';
import { Dropdown } from 'primereact/dropdown';
import { Calendar } from 'primereact/calendar';
import { Toast } from 'primereact/toast';

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
                const adminApi = new AdminControllerApi();

                const [absenceData, userData] = await Promise.all([
                    absenceApi.getAll8({ departmentId: myDept.id }).then(r => r.data),
                    adminApi.getAllUsers().then(r => r.data),
                ]);

                setAbsences(absenceData);
                const map: Record<number, UserxDTO> = {};
                userData.forEach(u => { if (u.id !== undefined) map[u.id] = u; });
                setUserMap(map);
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

    const employeeBody = (row: AbsenceDTO) => {
        if (row.userxId === undefined) return '—';
        const user = userMap[row.userxId];
        if (!user) return `User ${row.userxId}`;
        const name = `${user.firstName ?? ''} ${user.lastName ?? ''}`.trim();
        return name || user.username || `User ${row.userxId}`;
    };

    const periodBody = (row: AbsenceDTO) => {
        const from = row.startDate ? new Date(row.startDate).toLocaleDateString() : '?';
        const to = row.endDate ? new Date(row.endDate).toLocaleDateString() : '?';
        return `${from} – ${to}`;
    };

    const statusBody = (row: AbsenceDTO) => {
        if (!row.absenceStatus) return null;
        return <Tag value={row.absenceStatus} severity={STATUS_SEVERITY[row.absenceStatus] ?? 'info'} />;
    };

    const actionsBody = (row: AbsenceDTO) => {
        if (row.absenceStatus !== AbsenceDTOAbsenceStatusEnum.PLANNED) return null;
        return (
            <div style={{ display: 'flex', gap: '0.5rem' }}>
                <Button
                    label="Approve"
                    icon="pi pi-check"
                    severity="success"
                    size="small"
                    onClick={() => updateStatus(row, AbsenceUpdateDTOAbsenceStatusEnum.APPROVED)}
                />
                <Button
                    label="Reject"
                    icon="pi pi-times"
                    severity="danger"
                    size="small"
                    onClick={() => updateStatus(row, AbsenceUpdateDTOAbsenceStatusEnum.REJECTED)}
                />
            </div>
        );
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

            <div style={{ padding: '1.5rem 2rem' }}>
                <h2 style={{ margin: '0 0 0.25rem', color: '#111827' }}>Team Absences</h2>
                {department && (
                    <p style={{ margin: '0 0 1.5rem', color: '#6b7280', fontSize: '0.95rem' }}>
                        <i className="pi pi-sitemap" style={{ marginRight: '0.4rem' }} />
                        {department.name}
                    </p>
                )}

                <div style={{ display: 'flex', gap: '1rem', marginBottom: '1.5rem', flexWrap: 'wrap', alignItems: 'flex-end' }}>
                    <div>
                        <label className="font-bold block" style={{ marginBottom: '0.3rem' }}>Status</label>
                        <Dropdown
                            value={statusFilter}
                            onChange={e => setStatusFilter(e.value)}
                            options={statusOptions}
                            placeholder="All statuses"
                            showClear
                            style={{ minWidth: '170px' }}
                        />
                    </div>
                    <div>
                        <label className="font-bold block" style={{ marginBottom: '0.3rem' }}>Date Range</label>
                        <Calendar
                            value={dateRange}
                            onChange={e => setDateRange((e.value as [Date | null, Date | null]) ?? [null, null])}
                            selectionMode="range"
                            readOnlyInput
                            placeholder="Filter by period"
                            showButtonBar
                            style={{ minWidth: '220px' }}
                        />
                    </div>
                </div>

                <DataTable
                    value={filteredAbsences}
                    emptyMessage="No absences found."
                    stripedRows
                    sortField="startDate"
                    sortOrder={-1}
                >
                    <Column header="Employee" body={employeeBody} />
                    <Column header="Period" body={periodBody} />
                    <Column field="absenceType" header="Type" />
                    <Column header="Status" body={statusBody} />
                    <Column header="Actions" body={actionsBody} style={{ minWidth: '14rem' }} />
                </DataTable>
            </div>

            <FooterComponent />
        </div>
    );
};

export default DepartmentAbsenceView;

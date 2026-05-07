import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ProgressSpinner } from 'primereact/progressspinner';
import { Message } from 'primereact/message';
import { Button } from 'primereact/button';
import 'primeicons/primeicons.css';

import NavbarComponent from '../components/NavbarComponent';
import RoomCard from '../components/RoomCard';
import { useUser } from '../Contexts/AuthenticatedUserContext';
import { DepartmentService } from '../services/DepartmentService';
import { MeasurementService } from '../services/MeasurementService';
import { ViolationService, ViolationStatusEnum } from '../services/ViolationService';
import { DepartmentDTO, MeasurementDTO, RoomDTO, ThresholdViolationDTO } from '../generated-skeleton-api';
import { ROUTES } from '../utilities/routes.paths';

const DepartmentDashboard: React.FC = () => {
    const { fullUser, currentUser } = useUser();
    const navigate = useNavigate();

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [department, setDepartment] = useState<DepartmentDTO | null>(null);
    const [rooms, setRooms] = useState<RoomDTO[]>([]);
    const [measurementsByRoom, setMeasurementsByRoom] = useState<Record<number, MeasurementDTO[]>>({});
    const [violationsByRoom, setViolationsByRoom] = useState<Record<number, ThresholdViolationDTO[]>>({});

    useEffect(() => {
        const load = async () => {
            try {
                const userId = fullUser?.id;
                if (!userId) {
                    setError('Could not determine current user.');
                    return;
                }

                // Find the department where this user is the lead
                const allDepts = await DepartmentService.getAll();
                const myDept = allDepts.find(d => d.departmentLeadId === userId);
                if (!myDept?.id) {
                    setError('No department found for this department lead.');
                    return;
                }
                setDepartment(myDept);

                // Fetch all rooms and active violations in parallel
                const [deptRooms, allViolations] = await Promise.all([
                    DepartmentService.getRooms(myDept.id),
                    ViolationService.getAll({
                        violationStatus: ViolationStatusEnum.ACTIVE,
                        departmentId: myDept.id,
                    }),
                ]);
                setRooms(deptRooms);

                // Load latest measurements for every room in parallel
                if (deptRooms.length > 0) {
                    const allMeasurements = await Promise.all(
                        deptRooms.map(r =>
                            r.id
                                ? MeasurementService.getLatestPerMetric(r.id).then(m => Object.values(m))
                                : Promise.resolve([]),
                        ),
                    );

                    const measMap: Record<number, MeasurementDTO[]> = {};
                    const violMap: Record<number, ThresholdViolationDTO[]> = {};
                    deptRooms.forEach((r, i) => {
                        if (r.id !== undefined) {
                            measMap[r.id] = allMeasurements[i];
                            violMap[r.id] = allViolations.filter(v => v.roomId === r.id);
                        }
                    });
                    setMeasurementsByRoom(measMap);
                    setViolationsByRoom(violMap);
                }
            } catch (err: unknown) {
                const msg = err instanceof Error ? err.message : String(err);
                console.error('Department dashboard load error:', err);
                setError(`Failed to load department data: ${msg}`);
            } finally {
                setLoading(false);
            }
        };
        void load();
    }, [fullUser?.id]);

    const totalActiveViolations = Object.values(violationsByRoom).flat().length;

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

            <div style={{ padding: '1.5rem 2rem 0' }}>
                {/* Page header */}
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1rem' }}>
                    <div>
                        <h2 style={{ margin: '0 0 0.25rem', color: '#111827' }}>
                            Welcome{currentUser?.firstName ? `, ${currentUser.firstName}` : ''}
                        </h2>
                        {department && (
                            <p style={{ margin: 0, color: '#6b7280', fontSize: '0.95rem' }}>
                                <i className="pi pi-sitemap" style={{ marginRight: '0.4rem' }} />
                                {department.name}
                            </p>
                        )}
                    </div>

                    <Button
                        label="Team Absences"
                        icon="pi pi-calendar-times"
                        className="p-button-outlined"
                        onClick={() => navigate(ROUTES.DEPARTMENT_ABSENCES)}
                        style={{ borderColor: '#0369a1', color: '#0369a1' }}
                    />
                </div>

                {/* Global violation banner */}
                {totalActiveViolations > 0 && (
                    <div style={{ marginBottom: '1rem' }}>
                        <Message
                            severity="warn"
                            text={`${totalActiveViolations} active threshold violation${totalActiveViolations > 1 ? 's' : ''} in your department.`}
                        />
                    </div>
                )}

                {/* Section title */}
                <div style={{
                    display: 'flex',
                    borderBottom: '2px solid #e5e7eb',
                    paddingBottom: '0.6rem',
                    marginBottom: '0',
                }}>
                    <div style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: '0.4rem',
                        padding: '0.6rem 1.25rem',
                        borderBottom: '2px solid #0369a1',
                        marginBottom: '-2px',
                        fontWeight: 600,
                        color: '#0369a1',
                        fontSize: '0.95rem',
                    }}>
                        <i className="pi pi-building" style={{ fontSize: '0.9rem' }} />
                        All Rooms
                        <span style={{
                            marginLeft: '0.5rem',
                            fontSize: '0.8rem',
                            fontWeight: 400,
                            color: '#9ca3af',
                        }}>
                            ({rooms.length})
                        </span>
                    </div>
                </div>
            </div>

            {/* Room grid */}
            <div style={{ padding: '2rem' }}>
                {rooms.length === 0 ? (
                    <div style={{
                        padding: '2rem',
                        textAlign: 'center',
                        background: '#f9fafb',
                        borderRadius: '8px',
                        border: '1px dashed #d1d5db',
                    }}>
                        <i className="pi pi-inbox" style={{ fontSize: '2rem', color: '#d1d5db', display: 'block', marginBottom: '0.5rem' }} />
                        <p style={{ color: '#6b7280', margin: '0.5rem 0 0' }}>No rooms assigned to this department.</p>
                    </div>
                ) : (
                    <div className="flex flex-wrap gap-3">
                        {rooms.map(room => (
                            <RoomCard
                                key={room.id}
                                room={room}
                                measurements={measurementsByRoom[room.id!] ?? []}
                                violations={violationsByRoom[room.id!] ?? []}
                            />
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
};

export default DepartmentDashboard;

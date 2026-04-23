import React, { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ProgressSpinner } from 'primereact/progressspinner';
import { Message } from 'primereact/message';
import 'primeicons/primeicons.css';

import NavbarComponent from '../components/NavbarComponent';
import RoomCard from '../components/RoomCard';
import { useUser } from '../Contexts/AuthenticatedUserContext';
import { ROUTES } from '../utilities/routes.paths';
import { EmployeeProfileService } from '../services/EmployeeProfileService';
import { MeasurementService } from '../services/MeasurementService';
import { RoomService } from '../services/RoomService';
import { ViolationService } from '../services/ViolationService';
import { DepartmentService } from '../services/DepartmentService';
import { UserService } from '../services/UserService';
import {
    MeasurementDTO,
    RoomDTO,
    RoomType,
    ThresholdViolationDTO,
    ThresholdViolationDTOViolationStatusEnum,
} from '../generated-skeleton-api';

const EmployeeDashboard: React.FC = () => {
    const { currentUser } = useUser();
    const navigate = useNavigate();

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [officeRoom, setOfficeRoom] = useState<RoomDTO | null>(null);
    const [commonRooms, setCommonRooms] = useState<RoomDTO[]>([]);
    const [measurementsByRoom, setMeasurementsByRoom] = useState<Record<number, MeasurementDTO[]>>({});
    const [violationsByRoom, setViolationsByRoom] = useState<Record<number, ThresholdViolationDTO[]>>({});

    const officeRef = useRef<HTMLDivElement>(null);
    const commonRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        const load = async () => {
            try {
                // JWT has no numeric id claim — fetch it from /api/userx/me
                const user = await UserService.getCurrentUser();
                if (!user.id) {
                    setError('Could not load user profile.');
                    return;
                }

                const profiles = await EmployeeProfileService.getAll();
                const profile = profiles.find(p => p.userxId === user.id);
                if (!profile?.roomId) {
                    setError('No employee profile with an assigned room found.');
                    return;
                }

                const [room, latestOffice, allViolations] = await Promise.all([
                    RoomService.getById(profile.roomId),
                    MeasurementService.getLatestPerMetric(profile.roomId),
                    ViolationService.getAll(),
                ]);

                setOfficeRoom(room);

                const activeForRoom = (roomId: number): ThresholdViolationDTO[] =>
                    allViolations.filter(
                        v =>
                            v.roomId === roomId &&
                            v.violationStatus === ThresholdViolationDTOViolationStatusEnum.ACTIVE,
                    );

                setMeasurementsByRoom({ [profile.roomId]: Object.values(latestOffice) });
                setViolationsByRoom({ [profile.roomId]: activeForRoom(profile.roomId) });

                if (profile.departmentId) {
                    const deptRooms = await DepartmentService.getRooms(profile.departmentId);
                    const common = deptRooms.filter(r => r.roomType === RoomType.COMMON_AREAS);
                    setCommonRooms(common);

                    if (common.length > 0) {
                        const commonMeasurements = await Promise.all(
                            common.map(r =>
                                r.id
                                    ? MeasurementService.getLatestPerMetric(r.id).then(m => Object.values(m))
                                    : Promise.resolve([]),
                            ),
                        );
                        const measMap: Record<number, MeasurementDTO[]> = {};
                        const violMap: Record<number, ThresholdViolationDTO[]> = {};
                        common.forEach((r, i) => {
                            if (r.id !== undefined) {
                                measMap[r.id] = commonMeasurements[i];
                                violMap[r.id] = activeForRoom(r.id);
                            }
                        });
                        setMeasurementsByRoom(prev => ({ ...prev, ...measMap }));
                        setViolationsByRoom(prev => ({ ...prev, ...violMap }));
                    }
                }
            } catch {
                setError('Failed to load room data.');
            } finally {
                setLoading(false);
            }
        };
        void load();
    }, []);

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
            <div className="flex" style={{ minHeight: 'calc(100vh - 60px)' }}>

                {/* Sidebar */}
                <nav style={{
                    width: '200px',
                    flexShrink: 0,
                    borderRight: '1px solid #e5e7eb',
                    padding: '1.5rem 0',
                    backgroundColor: '#f9fafb',
                }}>
                    <SidebarItem
                        label="Office"
                        onClick={() => officeRef.current?.scrollIntoView({ behavior: 'smooth' })}
                        active
                    />
                    <SidebarItem
                        label="Common Areas"
                        onClick={() => commonRef.current?.scrollIntoView({ behavior: 'smooth' })}
                    />
                    <SidebarItem
                        label="Absences"
                        onClick={() => navigate(ROUTES.ABSENCE)}
                    />
                </nav>

                {/* Main content */}
                <main style={{ flex: 1, padding: '1.5rem 2rem', overflowY: 'auto' }}>
                    <h2 className="mt-0 mb-3" style={{ color: '#111827' }}>
                        Welcome{currentUser?.firstName ? `, ${currentUser.firstName}` : ''}
                    </h2>

                    {totalActiveViolations > 0 && (
                        <div className="mb-4">
                            <Message
                                severity="warn"
                                text={`${totalActiveViolations} active threshold violation${totalActiveViolations > 1 ? 's' : ''} in your rooms.`}
                            />
                        </div>
                    )}

                    <section ref={officeRef} className="mb-5">
                        <h3 className="mt-0 mb-3" style={{ color: '#374151' }}>
                            <i className="pi pi-building mr-2" />
                            My Office
                        </h3>
                        {officeRoom ? (
                            <RoomCard
                                room={officeRoom}
                                measurements={measurementsByRoom[officeRoom.id!] ?? []}
                                violations={violationsByRoom[officeRoom.id!] ?? []}
                            />
                        ) : (
                            <p style={{ color: '#6b7280' }}>No office assigned.</p>
                        )}
                    </section>

                    <section ref={commonRef}>
                        <h3 className="mt-0 mb-3" style={{ color: '#374151' }}>
                            <i className="pi pi-th-large mr-2" />
                            Common Areas
                        </h3>
                        {commonRooms.length === 0 ? (
                            <p style={{ color: '#6b7280' }}>No common areas in your department.</p>
                        ) : (
                            <div className="flex flex-wrap gap-3">
                                {commonRooms.map(room => (
                                    <RoomCard
                                        key={room.id}
                                        room={room}
                                        measurements={measurementsByRoom[room.id!] ?? []}
                                        violations={violationsByRoom[room.id!] ?? []}
                                    />
                                ))}
                            </div>
                        )}
                    </section>
                </main>
            </div>
        </div>
    );
};

interface SidebarItemProps {
    label: string;
    onClick: () => void;
    active?: boolean;
}

const SidebarItem: React.FC<SidebarItemProps> = ({ label, onClick, active = false }) => (
    <button
        onClick={onClick}
        style={{
            display: 'block',
            width: '100%',
            textAlign: 'left',
            padding: '0.75rem 1.25rem',
            border: 'none',
            background: active ? '#e0f2fe' : 'transparent',
            borderLeft: active ? '3px solid #0ea5e9' : '3px solid transparent',
            cursor: 'pointer',
            fontSize: '0.95rem',
            fontWeight: active ? 600 : 400,
            color: active ? '#0369a1' : '#374151',
        }}
    >
        {label}
    </button>
);

export default EmployeeDashboard;

import React, { useEffect, useState } from 'react';
import { ProgressSpinner } from 'primereact/progressspinner';
import { Message } from 'primereact/message';
import 'primeicons/primeicons.css';

import NavbarComponent from '../components/NavbarComponent';
import RoomCard from '../components/RoomCard';
import { useUser } from '../Contexts/AuthenticatedUserContext';
import { EmployeeProfileService } from '../services/EmployeeProfileService';
import { MeasurementService } from '../services/MeasurementService';
import { RoomService } from '../services/RoomService';
import { ViolationService } from '../services/ViolationService';
import { DepartmentService } from '../services/DepartmentService';
import {
    MeasurementDTO,
    RoomDTO,
    RoomType,
    ThresholdViolationDTO,
    ThresholdViolationDTOViolationStatusEnum,
} from '../generated-skeleton-api';

type TabId = 'office' | 'common';

const TABS: { id: TabId; label: string; icon: string }[] = [
    { id: 'office', label: 'My Office',   icon: 'pi pi-briefcase' },
    { id: 'common', label: 'Common Areas', icon: 'pi pi-building' },
];

const EmployeeDashboard: React.FC = () => {
    const { currentUser } = useUser();

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [activeTab, setActiveTab] = useState<TabId>('office');
    const [officeRoom, setOfficeRoom] = useState<RoomDTO | null>(null);
    const [commonRooms, setCommonRooms] = useState<RoomDTO[]>([]);
    const [measurementsByRoom, setMeasurementsByRoom] = useState<Record<number, MeasurementDTO[]>>({});
    const [violationsByRoom, setViolationsByRoom] = useState<Record<number, ThresholdViolationDTO[]>>({});

    useEffect(() => {
        const load = async () => {
            try {
                const profile = await EmployeeProfileService.getMe();
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
            } catch (err: unknown) {
                const msg = err instanceof Error ? err.message : String(err);
                console.error('Dashboard load error:', err);
                setError(`Failed to load room data: ${msg}`);
            } finally {
                setLoading(false);
            }
        };
        void load();
    }, []);

    const handleTabClick = (tab: TabId) => {
        setActiveTab(tab);
    };

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
                <h2 style={{ margin: '0 0 1rem', color: '#111827' }}>
                    Welcome{currentUser?.firstName ? `, ${currentUser.firstName}` : ''}
                </h2>

                {totalActiveViolations > 0 && (
                    <div style={{ marginBottom: '1rem' }}>
                        <Message
                            severity="warn"
                            text={`${totalActiveViolations} active threshold violation${totalActiveViolations > 1 ? 's' : ''} in your rooms.`}
                        />
                    </div>
                )}

                {/* Horizontal tab bar */}
                <div style={{
                    display: 'flex',
                    borderBottom: '2px solid #e5e7eb',
                    gap: '0.25rem',
                }}>
                    {TABS.map(tab => (
                        <button
                            key={tab.id}
                            onClick={() => handleTabClick(tab.id)}
                            style={{
                                display: 'flex',
                                alignItems: 'center',
                                gap: '0.4rem',
                                padding: '0.6rem 1.25rem',
                                border: 'none',
                                background: 'transparent',
                                cursor: 'pointer',
                                fontSize: '0.95rem',
                                fontWeight: activeTab === tab.id ? 600 : 400,
                                color: activeTab === tab.id ? '#0369a1' : '#374151',
                                borderBottom: activeTab === tab.id ? '2px solid #0369a1' : '2px solid transparent',
                                marginBottom: '-2px',
                            }}
                        >
                            <i className={tab.icon} style={{ fontSize: '0.9rem' }} />
                            {tab.label}
                        </button>
                    ))}
                </div>
            </div>

            {/* Content */}
            <div style={{ padding: '2rem' }}>
                {activeTab === 'office' && (
                    <section>
                        <h3 style={{ marginTop: 0, marginBottom: '1rem', color: '#374151' }}>My Office</h3>
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
                )}

                {activeTab === 'common' && (
                    <section>
                        <h3 style={{ marginTop: 0, marginBottom: '1rem', color: '#374151' }}>Common Areas</h3>
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
                )}
            </div>
        </div>
    );
};

export default EmployeeDashboard;

import React, { useEffect, useMemo, useState } from 'react';
import { ProgressSpinner } from 'primereact/progressspinner';
import { Message } from 'primereact/message';
import 'primeicons/primeicons.css';

import NavbarComponent from '../components/NavbarComponent';
import RoomCard from '../components/RoomCard';
import { useUser } from '../Contexts/AuthenticatedUserContext';
import { EmployeeProfileService } from '../services/EmployeeProfileService';
import { RoomService } from '../services/RoomService';
import { ViolationService, ViolationStatusEnum } from '../services/ViolationService';
import { DepartmentService } from '../services/DepartmentService';
import { AnalyticsService } from '../services/AnalyticsService';
import { registerDashboardCacheClearHandler } from '../utilities/dashboardCacheInvalidation';
import {
    EmployeeProfileDTO,
    MeasurementDTO,
    MeasurementDTOMetricEnum,
    RoomDTO,
    RoomType,
    ThresholdViolationDTO,
} from '../generated-skeleton-api';

type TabId = 'office' | 'common';
type HydrationPhase = 'commonRooms' | 'commonMeasurements' | 'complete';

interface HydrationStatus {
    phase: HydrationPhase;
    label: string;
    done: number;
    total: number;
}

const TABS: { id: TabId; label: string; icon: string }[] = [
    { id: 'office', label: 'My Office', icon: 'pi pi-briefcase' },
    { id: 'common', label: 'Common Areas', icon: 'pi pi-building' },
];

const cachedRoomRequestsById = new Map<number, Promise<RoomDTO>>();
const cachedCommonRoomsByDepartment = new Map<number, RoomDTO[]>();
const cachedCommonRoomRequestsByDepartment = new Map<number, Promise<RoomDTO[]>>();
const cachedActiveViolationsByDepartment = new Map<number, ThresholdViolationDTO[]>();
const cachedActiveViolationRequestsByDepartment = new Map<number, Promise<ThresholdViolationDTO[]>>();
const cachedActiveViolationsByRoom = new Map<number, ThresholdViolationDTO[]>();
const cachedActiveViolationRequestsByRoom = new Map<number, Promise<ThresholdViolationDTO[]>>();

function clearEmployeeDashboardCaches(): void {
    cachedRoomRequestsById.clear();
    cachedCommonRoomsByDepartment.clear();
    cachedCommonRoomRequestsByDepartment.clear();
    cachedActiveViolationsByDepartment.clear();
    cachedActiveViolationRequestsByDepartment.clear();
    cachedActiveViolationsByRoom.clear();
    cachedActiveViolationRequestsByRoom.clear();
}

registerDashboardCacheClearHandler(clearEmployeeDashboardCaches);

function delay(ms: number): Promise<void> {
    return new Promise(resolve => window.setTimeout(resolve, ms));
}

async function getRoomFresh(roomId: number): Promise<RoomDTO> {
    let request = cachedRoomRequestsById.get(roomId);
    if (!request) {
        request = RoomService.getById(roomId)
            .finally(() => {
                cachedRoomRequestsById.delete(roomId);
            });
        cachedRoomRequestsById.set(roomId, request);
    }
    return request;
}

async function getLatestMeasurementsFresh(roomId: number): Promise<MeasurementDTO[]> {
    const summary = await AnalyticsService.getRoomSummary(roomId);
    const timestamp = summary.generatedAt;
    const metrics = summary.metrics ?? {};

    return Object.values(MeasurementDTOMetricEnum)
        .flatMap(metric => {
            const latest = metrics[metric]?.latest;
            if (latest === undefined || latest === null) return [];

            return [{
                timestamp,
                measurement: latest,
                metric,
                roomId,
            }];
        });
}

async function getCommonRoomsCached(departmentId: number): Promise<RoomDTO[]> {
    const cached = cachedCommonRoomsByDepartment.get(departmentId);
    if (cached) return cached;

    let request = cachedCommonRoomRequestsByDepartment.get(departmentId);
    if (!request) {
        request = DepartmentService.getRooms(departmentId)
            .then(rooms => {
                const commonRooms = rooms.filter(room => room.roomType === RoomType.COMMON_AREAS);
                cachedCommonRoomsByDepartment.set(departmentId, commonRooms);
                return commonRooms;
            })
            .finally(() => {
                cachedCommonRoomRequestsByDepartment.delete(departmentId);
            });
        cachedCommonRoomRequestsByDepartment.set(departmentId, request);
    }
    return request;
}

async function getActiveViolationsForDepartmentCached(departmentId: number): Promise<ThresholdViolationDTO[]> {
    const cached = cachedActiveViolationsByDepartment.get(departmentId);
    if (cached) return cached;

    let request = cachedActiveViolationRequestsByDepartment.get(departmentId);
    if (!request) {
        request = ViolationService.getAll({
            violationStatus: ViolationStatusEnum.ACTIVE,
            departmentId,
        })
            .then(violations => {
                cachedActiveViolationsByDepartment.set(departmentId, violations);
                return violations;
            })
            .finally(() => {
                cachedActiveViolationRequestsByDepartment.delete(departmentId);
            });
        cachedActiveViolationRequestsByDepartment.set(departmentId, request);
    }
    return request;
}

async function getActiveViolationsForRoomCached(roomId: number): Promise<ThresholdViolationDTO[]> {
    const cached = cachedActiveViolationsByRoom.get(roomId);
    if (cached) return cached;

    let request = cachedActiveViolationRequestsByRoom.get(roomId);
    if (!request) {
        request = ViolationService.getAll({
            violationStatus: ViolationStatusEnum.ACTIVE,
            roomId,
        })
            .then(violations => {
                cachedActiveViolationsByRoom.set(roomId, violations);
                return violations;
            })
            .finally(() => {
                cachedActiveViolationRequestsByRoom.delete(roomId);
            });
        cachedActiveViolationRequestsByRoom.set(roomId, request);
    }
    return request;
}

const EmployeeDashboard: React.FC = () => {
    const { currentUser } = useUser();

    const [loadingOffice, setLoadingOffice] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [activeTab, setActiveTab] = useState<TabId>('office');
    const [profile, setProfile] = useState<EmployeeProfileDTO | null>(null);
    const [officeRoom, setOfficeRoom] = useState<RoomDTO | null>(null);
    const [commonRooms, setCommonRooms] = useState<RoomDTO[] | null>(null);
    const [measurementsByRoom, setMeasurementsByRoom] = useState<Record<number, MeasurementDTO[]>>({});
    const [violationsByRoom, setViolationsByRoom] = useState<Record<number, ThresholdViolationDTO[]>>(() =>
        Object.fromEntries(cachedActiveViolationsByRoom.entries()),
    );
    const [hydrationStatus, setHydrationStatus] = useState<HydrationStatus | null>(null);

    useEffect(() => {
        let active = true;

        const loadOffice = async () => {
            try {
                setError(null);
                const employeeProfile = await EmployeeProfileService.getMe();
                if (!active) return;
                setProfile(employeeProfile);

                if (!employeeProfile?.roomId) {
                    setError('No employee profile with an assigned room found.');
                    return;
                }

                const room = await getRoomFresh(employeeProfile.roomId);
                if (!active) return;
                setOfficeRoom(room);

                let latestOffice: MeasurementDTO[] = [];
                try {
                    latestOffice = await getLatestMeasurementsFresh(employeeProfile.roomId);
                } catch (err: unknown) {
                    const status = (err as { response?: { status?: number } })?.response?.status;
                    if (status === 403) {
                        if (active) setError('Klimadaten nicht verfügbar — Datenschutz aktiv (Belegung unter Mindestanzahl).');
                    } else {
                        throw err;
                    }
                }
                if (!active) return;

                const officeViolations = employeeProfile.departmentId
                    ? await getActiveViolationsForDepartmentCached(employeeProfile.departmentId)
                    : await getActiveViolationsForRoomCached(employeeProfile.roomId);
                if (!active) return;

                setMeasurementsByRoom(prev => ({ ...prev, [employeeProfile.roomId!]: latestOffice }));
                setViolationsByRoom(prev => ({
                    ...prev,
                    [employeeProfile.roomId!]: officeViolations.filter(violation => violation.roomId === employeeProfile.roomId),
                }));
            } catch (err: unknown) {
                const msg = err instanceof Error ? err.message : String(err);
                console.error('Dashboard load error:', err);
                if (active) {
                    setError(`Failed to load office data: ${msg}`);
                }
            } finally {
                if (active) setLoadingOffice(false);
            }
        };

        void loadOffice();
        return () => {
            active = false;
        };
    }, []);

    useEffect(() => {
        if (!profile?.departmentId) return;
        let active = true;

        const hydrateCommonAreas = async () => {
            try {
                setHydrationStatus({
                    phase: 'commonRooms',
                    label: 'Loading common areas for your department',
                    done: 0,
                    total: 1,
                });

                if (!cachedCommonRoomsByDepartment.has(profile.departmentId!)) {
                    await delay(650);
                }

                const rooms = await getCommonRoomsCached(profile.departmentId!);
                if (!active) return;

                setCommonRooms(rooms);
                setHydrationStatus({
                    phase: 'commonRooms',
                    label: 'Loaded common areas',
                    done: 1,
                    total: 1,
                });

                const departmentViolations = await getActiveViolationsForDepartmentCached(profile.departmentId!);
                if (!active) return;

                for (let i = 0; i < rooms.length; i++) {
                    const room = rooms[i];
                    if (room.id === undefined) continue;

                    setHydrationStatus({
                        phase: 'commonMeasurements',
                        label: `Loading data for ${room.name ?? `Room ${room.id}`}`,
                        done: i,
                        total: rooms.length,
                    });

                    await delay(850);

                    const measurements = await getLatestMeasurementsFresh(room.id);
                    if (!active) return;

                    setMeasurementsByRoom(prev => ({ ...prev, [room.id!]: measurements }));
                    setViolationsByRoom(prev => ({
                        ...prev,
                        [room.id!]: departmentViolations.filter(violation => violation.roomId === room.id),
                    }));
                    setHydrationStatus({
                        phase: 'commonMeasurements',
                        label: `Loaded ${room.name ?? `Room ${room.id}`}`,
                        done: i + 1,
                        total: rooms.length,
                    });
                }

                if (active) {
                    setHydrationStatus({
                        phase: 'complete',
                        label: 'Background loading complete',
                        done: rooms.length,
                        total: rooms.length,
                    });
                }
            } catch (err: unknown) {
                const msg = err instanceof Error ? err.message : String(err);
                console.error('Common area hydration error:', err);
                if (active) setError(`Failed to load common areas: ${msg}`);
            }
        };

        void hydrateCommonAreas();
        return () => {
            active = false;
        };
    }, [profile?.departmentId]);

    const handleTabClick = (tab: TabId) => {
        setActiveTab(tab);
    };

    const totalActiveViolations = useMemo(
        () => Object.values(violationsByRoom).flat().length,
        [violationsByRoom],
    );

    if (loadingOffice) {
        return (
            <div>
                <NavbarComponent />
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '60vh' }}>
                    <ProgressSpinner />
                </div>
            </div>
        );
    }

    if (error && !officeRoom) {
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

                {error && (
                    <div style={{ marginBottom: '1rem' }}>
                        <Message severity="error" text={error} />
                    </div>
                )}

                {hydrationStatus && (
                    <div
                        style={{
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'space-between',
                            gap: '1rem',
                            background: hydrationStatus.phase === 'complete' ? '#f0fdf4' : '#eff6ff',
                            border: `1px solid ${hydrationStatus.phase === 'complete' ? '#bbf7d0' : '#bfdbfe'}`,
                            color: hydrationStatus.phase === 'complete' ? '#166534' : '#1e3a8a',
                            borderRadius: '8px',
                            padding: '0.65rem 0.9rem',
                            marginBottom: '1rem',
                            fontSize: '0.9rem',
                        }}
                    >
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.55rem' }}>
                            {hydrationStatus.phase === 'complete' ? (
                                <i className="pi pi-check-circle" />
                            ) : (
                                <ProgressSpinner style={{ width: '1rem', height: '1rem' }} strokeWidth="8" />
                            )}
                            <span>{hydrationStatus.label}</span>
                        </div>
                        <span style={{ fontVariantNumeric: 'tabular-nums', whiteSpace: 'nowrap' }}>
                            {hydrationStatus.total > 0 ? `${hydrationStatus.done}/${hydrationStatus.total}` : ''}
                        </span>
                    </div>
                )}

                {totalActiveViolations > 0 && (
                    <div style={{ marginBottom: '1rem' }}>
                        <Message
                            severity="warn"
                            text={`${totalActiveViolations} active threshold violation${totalActiveViolations > 1 ? 's' : ''} in your loaded rooms.`}
                        />
                    </div>
                )}

                <div style={{ display: 'flex', borderBottom: '2px solid #e5e7eb', gap: '0.25rem' }}>
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
                        {commonRooms === null ? (
                            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', color: '#6b7280' }}>
                                <ProgressSpinner style={{ width: '1.5rem', height: '1.5rem' }} />
                                <span>Loading common areas...</span>
                            </div>
                        ) : commonRooms.length === 0 ? (
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

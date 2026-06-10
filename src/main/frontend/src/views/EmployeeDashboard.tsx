import React, { useEffect, useMemo, useState } from 'react';
import { addDays, format } from 'date-fns';
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

const TAB_STYLE = (active: boolean): React.CSSProperties => ({
    padding: '0.8rem 1.5rem',
    border: 'none',
    borderBottom: active ? '3px solid #0369a1' : '3px solid transparent',
    background: active ? '#f0f9ff' : 'transparent',
    cursor: 'pointer',
    fontWeight: active ? 700 : 500,
    color: active ? '#0369a1' : '#6b7280',
    fontSize: '0.95rem',
    transition: 'all 0.2s ease',
    borderRadius: '8px 8px 0 0',
});

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

const toLocalDateTimeParam = (date: Date): string => format(date, "yyyy-MM-dd'T'HH:mm:ss");

function todayWindow(): { from: string; to: string } {
    const now = new Date();
    const from = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    return {
        from: toLocalDateTimeParam(from),
        to: toLocalDateTimeParam(addDays(from, 1)),
    };
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

    const measurements = Object.values(MeasurementDTOMetricEnum)
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

    if (measurements.length > 0) return measurements;

    const window = todayWindow();
    const trendMeasurements: MeasurementDTO[] = [];

    await Promise.all(Object.values(MeasurementDTOMetricEnum).map(async metric => {
        const trend = await AnalyticsService.getRoomTrend(roomId, metric, window.from, window.to);
        const latestPoint = [...(trend.points ?? [])]
            .filter(point => point.timestamp && point.value !== undefined && point.value !== null)
            .sort((a, b) => new Date(b.timestamp!).getTime() - new Date(a.timestamp!).getTime())[0];

        if (!latestPoint) return;

        trendMeasurements.push({
            timestamp: latestPoint.timestamp,
            measurement: latestPoint.value,
            metric,
            roomId,
        });
    }));

    return trendMeasurements;
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

            <div style={{ padding: '1.5rem 2rem', maxWidth: '1400px', margin: '0 auto' }}>
                {/* Header */}
                <div style={{ marginBottom: '2rem', padding: '1.5rem 2rem', backgroundColor: '#f8f9fa', borderRadius: '12px', border: '1px solid #e9ecef', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
                    <h1 style={{ margin: 0, color: '#111827', fontSize: '2rem', fontWeight: 700 }}>
                        Welcome{currentUser?.firstName ? `, ${currentUser.firstName}` : ''}
                    </h1>
                </div>

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

                {/* Tabs */}
                <div style={{ display: 'flex', borderBottom: '2px solid #e5e7eb', marginBottom: '2rem', backgroundColor: '#ffffff', borderRadius: '12px 12px 0 0', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
                    {TABS.map(tab => (
                        <button key={tab.id} style={TAB_STYLE(activeTab === tab.id)} onClick={() => handleTabClick(tab.id)}>
                            <i className={tab.icon} style={{ marginRight: '0.5rem' }} />
                            {tab.label}
                        </button>
                    ))}
                </div>

                {/* Content */}
                <div style={{ padding: '2rem', backgroundColor: '#ffffff', borderRadius: '0 12px 12px 12px', border: '1px solid #e5e7eb', borderTop: 'none' }}>
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
        </div>
    );
};

export default EmployeeDashboard;

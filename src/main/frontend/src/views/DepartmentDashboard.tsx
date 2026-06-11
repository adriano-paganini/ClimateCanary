import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { addDays, format } from 'date-fns';
import { ProgressSpinner } from 'primereact/progressspinner';
import { Message } from 'primereact/message';
import { Button } from 'primereact/button';
import { Dropdown } from 'primereact/dropdown';
import 'primeicons/primeicons.css';

import NavbarComponent from '../components/NavbarComponent';
import RoomCard from '../components/RoomCard';
import { useUser } from '../Contexts/AuthenticatedUserContext';
import { useDepartment } from '../Contexts/DepartmentContext';
import { ViolationService, ViolationStatusEnum } from '../services/ViolationService';
import { DepartmentService } from '../services/DepartmentService';
import { AnalyticsService } from '../services/AnalyticsService';
import { MeasurementDTO, MeasurementDTOMetricEnum, RoomDTO, ThresholdViolationDTO } from '../generated-skeleton-api';
import { ROUTES } from '../utilities/routes.paths';
import { registerDashboardCacheClearHandler } from '../utilities/dashboardCacheInvalidation';

interface RoomDashboardData {
    measurements: MeasurementDTO[];
    violations: ThresholdViolationDTO[];
}

interface CachedRoomDashboardData {
    data: RoomDashboardData;
    fetchedAt: number;
}

interface HydrationStatus {
    label: string;
    done: number;
    total: number;
}

const ROOM_DATA_CACHE_TTL_MS = 15_000;

const cachedRoomsByDepartment = new Map<number, RoomDTO[]>();
const cachedRoomRequestsByDepartment = new Map<number, Promise<RoomDTO[]>>();
const cachedRoomData = new Map<number, CachedRoomDashboardData>();
const cachedRoomDataRequests = new Map<number, Promise<RoomDashboardData>>();

function clearDepartmentDashboardCaches(): void {
    cachedRoomsByDepartment.clear();
    cachedRoomRequestsByDepartment.clear();
    cachedRoomData.clear();
    cachedRoomDataRequests.clear();
}

registerDashboardCacheClearHandler(clearDepartmentDashboardCaches);

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

async function getRoomsCached(departmentId: number): Promise<RoomDTO[]> {
    const cached = cachedRoomsByDepartment.get(departmentId);
    if (cached) return cached;

    let request = cachedRoomRequestsByDepartment.get(departmentId);
    if (!request) {
        request = DepartmentService.getRooms(departmentId)
            .then(rooms => {
                cachedRoomsByDepartment.set(departmentId, rooms);
                return rooms;
            })
            .finally(() => {
                cachedRoomRequestsByDepartment.delete(departmentId);
            });
        cachedRoomRequestsByDepartment.set(departmentId, request);
    }

    return request;
}

async function loadRoomDashboardData(roomId: number): Promise<RoomDashboardData> {
    const [summary, violations] = await Promise.all([
        AnalyticsService.getRoomSummary(roomId),
        ViolationService.getAll({
            roomId,
            violationStatus: ViolationStatusEnum.ACTIVE,
        }),
    ]);

    const metrics = summary.metrics ?? {};
    const measurements: MeasurementDTO[] = Object.values(MeasurementDTOMetricEnum)
        .flatMap(metric => {
            const latest = metrics[metric]?.latest;
            if (latest === undefined || latest === null) return [];

            return [{
                roomId,
                metric,
                measurement: latest,
                timestamp: summary.generatedAt,
            }];
        });

    return {
        measurements,
        violations,
    };
}

async function loadRoomDashboardDataWithTrendFallback(roomId: number): Promise<RoomDashboardData> {
    const data = await loadRoomDashboardData(roomId);
    if (data.measurements.length > 0) return data;

    const window = todayWindow();
    const trendMeasurements: MeasurementDTO[] = [];

    await Promise.all(Object.values(MeasurementDTOMetricEnum).map(async metric => {
        const trend = await AnalyticsService.getRoomTrend(roomId, metric, window.from, window.to);
        const latestPoint = [...(trend.points ?? [])]
            .filter(point => point.timestamp && point.value !== undefined && point.value !== null)
            .sort((a, b) => new Date(b.timestamp!).getTime() - new Date(a.timestamp!).getTime())[0];

        if (!latestPoint) return;

        trendMeasurements.push({
            roomId,
            metric,
            measurement: latestPoint.value,
            timestamp: latestPoint.timestamp,
        });
    }));

    return {
        measurements: trendMeasurements,
        violations: data.violations,
    };
}

function getFreshCachedRoomData(roomId: number): RoomDashboardData | null {
    const cached = cachedRoomData.get(roomId);
    if (!cached) return null;

    const isFresh = Date.now() - cached.fetchedAt < ROOM_DATA_CACHE_TTL_MS;
    return isFresh ? cached.data : null;
}

async function getRoomDashboardDataCached(roomId: number): Promise<RoomDashboardData> {
    const cached = getFreshCachedRoomData(roomId);
    if (cached) return cached;

    let request = cachedRoomDataRequests.get(roomId);
    if (!request) {
        request = loadRoomDashboardDataWithTrendFallback(roomId)
            .then(data => {
                if (data.measurements.length > 0) {
                    cachedRoomData.set(roomId, {
                        data,
                        fetchedAt: Date.now(),
                    });
                } else {
                    cachedRoomData.delete(roomId);
                }
                return data;
            })
            .finally(() => {
                cachedRoomDataRequests.delete(roomId);
            });
        cachedRoomDataRequests.set(roomId, request);
    }

    return request;
}

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

const DepartmentDashboard: React.FC = () => {
    const { currentUser } = useUser();
    const { departments, selectedDepartmentId, setSelectedDepartmentId, loading, error: deptError } = useDepartment();
    const navigate = useNavigate();

    const [loadingDepartmentData, setLoadingDepartmentData] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [rooms, setRooms] = useState<RoomDTO[]>([]);
    const [measurementsByRoom, setMeasurementsByRoom] = useState<Record<number, MeasurementDTO[]>>({});
    const [violationsByRoom, setViolationsByRoom] = useState<Record<number, ThresholdViolationDTO[]>>({});
    const [loadingRoomIds, setLoadingRoomIds] = useState<Set<number>>(new Set());
    const [hydrationStatus, setHydrationStatus] = useState<HydrationStatus | null>(null);

    useEffect(() => {
        if (!selectedDepartmentId) {
            setRooms([]);
            setMeasurementsByRoom({});
            setViolationsByRoom({});
            setLoadingRoomIds(new Set());
            setHydrationStatus(null);
            return;
        }

        let active = true;

        const loadDepartmentDataDynamically = async () => {
            setLoadingDepartmentData(true);
            setError(null);
            setRooms([]);
            setMeasurementsByRoom({});
            setViolationsByRoom({});
            setLoadingRoomIds(new Set());
            setHydrationStatus(null);

            try {
                const deptRooms = await getRoomsCached(selectedDepartmentId);
                if (!active) return;

                setRooms(deptRooms);
                setLoadingDepartmentData(false);

                const roomsWithIds = deptRooms.filter((room): room is RoomDTO & { id: number } => room.id !== undefined);
                if (roomsWithIds.length === 0) {
                    setHydrationStatus(null);
                    return;
                }

                for (let i = 0; i < roomsWithIds.length; i++) {
                    if (!active) return;

                    const room = roomsWithIds[i];
                    setHydrationStatus({
                        label: `Loading data for ${room.name ?? `Room ${room.id}`}`,
                        done: i,
                        total: roomsWithIds.length,
                    });
                    setLoadingRoomIds(prev => new Set(prev).add(room.id));

                    if (!getFreshCachedRoomData(room.id)) {
                        await delay(250);
                    }

                    const data = await getRoomDashboardDataCached(room.id);
                    if (!active) return;

                    setMeasurementsByRoom(prev => ({ ...prev, [room.id]: data.measurements }));
                    setViolationsByRoom(prev => ({ ...prev, [room.id]: data.violations }));
                    setLoadingRoomIds(prev => {
                        const next = new Set(prev);
                        next.delete(room.id);
                        return next;
                    });
                    setHydrationStatus({
                        label: `Loaded ${room.name ?? `Room ${room.id}`}`,
                        done: i + 1,
                        total: roomsWithIds.length,
                    });
                }
            } catch (err: unknown) {
                const msg = err instanceof Error ? err.message : String(err);
                console.error('Department dashboard data load error:', err);
                if (active) setError(`Failed to load department data: ${msg}`);
            } finally {
                if (active) {
                    setLoadingDepartmentData(false);
                    setLoadingRoomIds(new Set());
                }
            }
        };

        void loadDepartmentDataDynamically();

        return () => {
            active = false;
        };
    }, [selectedDepartmentId]);

    const totalActiveViolations = Object.values(violationsByRoom).flat().length;
    const department = departments.find(d => d.id === selectedDepartmentId) ?? null;
    const departmentOptions = departments.map(d => ({
        label: d.name ?? `Department ${d.id}`,
        value: d.id,
    }));

    const combinedError = error ?? deptError;

    if (loading) {
        return (
            <div>
                <NavbarComponent />
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '60vh' }}>
                    <ProgressSpinner />
                </div>
            </div>
        );
    }

    if (combinedError) {
        return (
            <div>
                <NavbarComponent />
                <div className="m-4">
                    <Message severity="error" text={combinedError} />
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
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '1rem' }}>
                        <div>
                            <h1 style={{ margin: '0 0 0.25rem', color: '#111827', fontSize: '2rem', fontWeight: 700 }}>
                                Welcome{currentUser?.firstName ? `, ${currentUser.firstName}` : ''}
                            </h1>
                            {departments.length > 1 ? (
                                <div style={{ marginTop: '0.75rem', maxWidth: '360px' }}>
                                    <Dropdown
                                        value={selectedDepartmentId}
                                        options={departmentOptions}
                                        onChange={e => setSelectedDepartmentId(e.value)}
                                        placeholder="Select department"
                                        style={{ width: '100%' }}
                                    />
                                </div>
                            ) : department && (
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

                {hydrationStatus && hydrationStatus.done < hydrationStatus.total && (
                    <div
                        style={{
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'space-between',
                            gap: '1rem',
                            background: '#eff6ff',
                            border: '1px solid #bfdbfe',
                            color: '#1e3a8a',
                            borderRadius: '8px',
                            padding: '0.65rem 0.9rem',
                            marginBottom: '1rem',
                            fontSize: '0.9rem',
                        }}
                    >
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.55rem' }}>
                            <i className="pi pi-spin pi-spinner" />
                            <span>{hydrationStatus.label}</span>
                        </div>
                        <span>{hydrationStatus.done}/{hydrationStatus.total}</span>
                    </div>
                )}

                {/* Tabs */}
                <div style={{ display: 'flex', borderBottom: '2px solid #e5e7eb', marginBottom: '2rem', backgroundColor: '#ffffff', borderRadius: '12px 12px 0 0', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
                    <div style={TAB_STYLE(true)}>
                        <i className="pi pi-building" style={{ marginRight: '0.5rem' }} />
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

                {/* Content */}
                <div style={{ padding: '2rem', backgroundColor: '#ffffff', borderRadius: '0 12px 12px 12px', border: '1px solid #e5e7eb', borderTop: 'none' }}>
                    {loadingDepartmentData ? (
                        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '20rem' }}>
                            <ProgressSpinner />
                        </div>
                    ) : rooms.length === 0 ? (
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
                            {rooms.map(room => {
                                const loadingRoom = room.id !== undefined && loadingRoomIds.has(room.id);

                                return (
                                    <div key={room.id} style={{ position: 'relative' }}>
                                        <RoomCard
                                            room={room}
                                            measurements={room.id !== undefined ? measurementsByRoom[room.id] ?? [] : []}
                                            violations={room.id !== undefined ? violationsByRoom[room.id] ?? [] : []}
                                            historyRoute={ROUTES.DEPARTMENT_ROOM_HISTORY}
                                        />
                                        {loadingRoom && (
                                            <div
                                                style={{
                                                    position: 'absolute',
                                                    inset: 0,
                                                    background: 'rgba(255,255,255,0.78)',
                                                    borderRadius: '10px',
                                                    display: 'flex',
                                                    alignItems: 'center',
                                                    justifyContent: 'center',
                                                    zIndex: 20,
                                                    color: '#1e3a8a',
                                                    fontWeight: 600,
                                                    pointerEvents: 'none',
                                                }}
                                            >
                                                <span style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                                    <i className="pi pi-spin pi-spinner" />
                                                    Loading...
                                                </span>
                                            </div>
                                        )}
                                    </div>
                                );
                            })}
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default DepartmentDashboard;

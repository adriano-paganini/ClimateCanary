import React, { useEffect, useMemo, useState } from "react";
import { ProgressSpinner } from "primereact/progressspinner";
import { Message } from "primereact/message";
import "primeicons/primeicons.css";

import NavbarComponent from "../components/NavbarComponent";
import NoDataOverlay from "../components/NoDataOverlay";
import { useUser } from "../Contexts/AuthenticatedUserContext";
import { DepartmentService } from "../services/DepartmentService";
import { MeasurementService } from "../services/MeasurementService";
import { hasDataGap } from "../utilities/dataGapUtils";
import {
  ViolationService,
  ViolationStatusEnum,
} from "../services/ViolationService";
import {
  DepartmentDTO,
  MeasurementDTO,
  MeasurementDTOMetricEnum,
  RoomDTO,
  ThresholdViolationDTO,
} from "../generated-skeleton-api";

type Period = "week" | "month";
type HydrationPhase = "rooms" | "roomData" | "complete";

interface TrendPoint {
  label: string;
  value: number;
}

interface RoomData {
  violations: ThresholdViolationDTO[];
  trends: Partial<Record<MeasurementDTOMetricEnum, TrendPoint[]>>;
  hasGaps: Partial<Record<MeasurementDTOMetricEnum, boolean>>;
}

interface HydrationStatus {
  phase: HydrationPhase;
  label: string;
  done: number;
  total: number;
}

const METRICS: {
  key: MeasurementDTOMetricEnum;
  label: string;
  unit: string;
  color: string;
}[] = [
  { key: MeasurementDTOMetricEnum.TEMPERATURE, label: "Temperature", unit: "°C", color: "#f59e0b" },
  { key: MeasurementDTOMetricEnum.HUMIDITY, label: "Humidity", unit: "%", color: "#0ea5e9" },
  { key: MeasurementDTOMetricEnum.IAQ, label: "Air Quality", unit: "IAQ", color: "#8b5cf6" },
];

let cachedDepartments: DepartmentDTO[] | null = null;
let cachedDepartmentsRequest: Promise<DepartmentDTO[]> | null = null;
const cachedRoomsByDepartment = new Map<number, RoomDTO[]>();
const cachedRoomRequestsByDepartment = new Map<number, Promise<RoomDTO[]>>();
const cachedRoomDataByPeriod = new Map<string, RoomData>();
const cachedRoomDataRequestsByPeriod = new Map<string, Promise<RoomData>>();

function violationColor(count: number): string {
  if (count === 0) return "#22c55e";
  if (count <= 2) return "#f59e0b";
  return "#ef4444";
}

function periodDays(period: Period): number {
  return period === "week" ? 7 : 30;
}

function roomDataKey(roomId: number, period: Period): string {
  return `${roomId}:${period}`;
}

function delay(ms: number): Promise<void> {
  return new Promise((resolve) => window.setTimeout(resolve, ms));
}

function bucketByDay(measurements: MeasurementDTO[], days: number): TrendPoint[] {
  const now = Date.now();
  const buckets: Record<string, number[]> = {};

  for (let i = days - 1; i >= 0; i--) {
    const d = new Date(now - i * 86_400_000);
    const key = d.toLocaleDateString("en-GB", { day: "2-digit", month: "2-digit" });
    buckets[key] = [];
  }

  measurements.forEach((m) => {
    if (m.measurement === undefined || !m.timestamp) return;
    const ts = new Date(m.timestamp).getTime();
    if (ts < now - days * 86_400_000) return;
    const key = new Date(m.timestamp).toLocaleDateString("en-GB", { day: "2-digit", month: "2-digit" });
    if (buckets[key] !== undefined) buckets[key].push(m.measurement);
  });

  return Object.entries(buckets)
    .map(([label, vals]) => ({
      label,
      value: vals.length > 0 ? vals.reduce((a, b) => a + b, 0) / vals.length : NaN,
    }))
    .filter((p) => !isNaN(p.value));
}

async function getDepartmentsCached(): Promise<DepartmentDTO[]> {
  if (cachedDepartments !== null) return cachedDepartments;
  if (cachedDepartmentsRequest === null) {
    cachedDepartmentsRequest = DepartmentService.getAll()
      .then((departments) => {
        cachedDepartments = departments;
        return departments;
      })
      .finally(() => {
        cachedDepartmentsRequest = null;
      });
  }
  return cachedDepartmentsRequest;
}

async function getRoomsCached(departmentId: number): Promise<RoomDTO[]> {
  const cached = cachedRoomsByDepartment.get(departmentId);
  if (cached) return cached;

  let request = cachedRoomRequestsByDepartment.get(departmentId);
  if (!request) {
    request = DepartmentService.getRooms(departmentId)
      .then((rooms) => {
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

async function getRoomDataCached(roomId: number, period: Period): Promise<RoomData> {
  const key = roomDataKey(roomId, period);
  const cached = cachedRoomDataByPeriod.get(key);
  if (cached) return cached;

  let request = cachedRoomDataRequestsByPeriod.get(key);
  if (!request) {
    request = loadRoomData(roomId, period)
      .then((data) => {
        cachedRoomDataByPeriod.set(key, data);
        return data;
      })
      .finally(() => {
        cachedRoomDataRequestsByPeriod.delete(key);
      });
    cachedRoomDataRequestsByPeriod.set(key, request);
  }
  return request;
}

async function loadRoomData(roomId: number, period: Period): Promise<RoomData> {
  const days = periodDays(period);
  const fromDate = new Date(Date.now() - days * 86_400_000);
  const from = fromDate.toISOString();
  const toDate = new Date();

  const [measurements, violations] = await Promise.all([
    MeasurementService.getAll({ roomId, from }),
    ViolationService.getAll({ roomId, violationStatus: ViolationStatusEnum.ACTIVE }),
  ]);

  const trends: Partial<Record<MeasurementDTOMetricEnum, TrendPoint[]>> = {};
  const hasGaps: Partial<Record<MeasurementDTOMetricEnum, boolean>> = {};
  METRICS.forEach(({ key }) => {
    const filtered = measurements.filter((m) => m.metric === key);
    const bucketed = bucketByDay(filtered, days);
    if (bucketed.length > 0) trends[key] = bucketed;
    hasGaps[key] = hasDataGap(filtered, fromDate, toDate);
  });

  return { violations, trends, hasGaps };
}

interface SparklineProps {
  uid: string;
  data: TrendPoint[];
  color: string;
  label: string;
  unit: string;
}

const Sparkline: React.FC<SparklineProps> = ({ uid, data, color, label, unit }) => {
  const W = 110;
  const H = 38;
  const PAD = 3;
  const values = data.map((d) => d.value);
  const min = Math.min(...values);
  const max = Math.max(...values);
  const range = max - min || 1;
  const latest = values[values.length - 1];
  const toX = (i: number) => PAD + (i / Math.max(values.length - 1, 1)) * (W - 2 * PAD);
  const toY = (v: number) => H - PAD - ((v - min) / range) * (H - 2 * PAD);
  const linePoints = values.map((v, i) => `${toX(i).toFixed(1)},${toY(v).toFixed(1)}`).join(" ");
  const areaPoints = [
    ...values.map((v, i) => `${toX(i).toFixed(1)},${toY(v).toFixed(1)}`),
    `${toX(values.length - 1).toFixed(1)},${H}`,
    `${PAD},${H}`,
  ].join(" ");
  const gradId = `spark-${uid}-${label.replace(/\s/g, "")}`;

  return (
    <div style={{ display: "flex", alignItems: "center", gap: "0.6rem" }}>
      <div>
        <div style={{ fontSize: "0.7rem", color: "#9ca3af", marginBottom: "2px" }}>{label}</div>
        <svg width={W} height={H}>
          <defs>
            <linearGradient id={gradId} x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor={color} stopOpacity={0.2} />
              <stop offset="100%" stopColor={color} stopOpacity={0} />
            </linearGradient>
          </defs>
          {values.length > 1 && (
            <>
              <polygon points={areaPoints} fill={`url(#${gradId})`} />
              <polyline points={linePoints} fill="none" stroke={color} strokeWidth="1.5" strokeLinejoin="round" strokeLinecap="round" />
            </>
          )}
          {values.length === 1 && <circle cx={W / 2} cy={H / 2} r={3} fill={color} />}
        </svg>
      </div>
      <div style={{ minWidth: "42px" }}>
        <div style={{ fontWeight: 700, fontSize: "0.95rem", color: "#111827", lineHeight: 1 }}>
          {latest.toFixed(1)}
        </div>
        <div style={{ fontSize: "0.7rem", color: "#9ca3af" }}>{unit}</div>
      </div>
    </div>
  );
};

const ManagementDashboard: React.FC = () => {
  const { currentUser } = useUser();
  const [departments, setDepartments] = useState<DepartmentDTO[]>(() => cachedDepartments ?? []);
  const [loadingDepartments, setLoadingDepartments] = useState(cachedDepartments === null);
  const [error, setError] = useState<string | null>(null);
  const [period, setPeriod] = useState<Period>("week");
  const [expandedDepartmentIds, setExpandedDepartmentIds] = useState<Set<number>>(new Set());
  const [roomsByDepartment, setRoomsByDepartment] = useState<Record<number, RoomDTO[]>>(() =>
    Object.fromEntries(cachedRoomsByDepartment.entries()),
  );
  const [loadingRooms, setLoadingRooms] = useState<Set<number>>(new Set());
  const [selectedRoomId, setSelectedRoomId] = useState<number | null>(null);
  const [roomDataByKey, setRoomDataByKey] = useState<Record<string, RoomData>>(() =>
    Object.fromEntries(cachedRoomDataByPeriod.entries()),
  );
  const [loadingRoomDataKey, setLoadingRoomDataKey] = useState<string | null>(null);
  const [hydrationStatus, setHydrationStatus] = useState<HydrationStatus | null>(null);

  useEffect(() => {
    let active = true;
    setLoadingDepartments(cachedDepartments === null);
    setError(null);

    getDepartmentsCached()
      .then((data) => {
        if (active) setDepartments(data);
      })
      .catch((err: unknown) => {
        const msg = err instanceof Error ? err.message : String(err);
        if (active) setError(`Failed to load departments: ${msg}`);
      })
      .finally(() => {
        if (active) setLoadingDepartments(false);
      });

    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    if (selectedRoomId == null) return;
    const key = roomDataKey(selectedRoomId, period);
    if (roomDataByKey[key]) return;

    let active = true;
    setLoadingRoomDataKey(key);
    setError(null);

    getRoomDataCached(selectedRoomId, period)
      .then((data) => {
        if (active) setRoomDataByKey((prev) => ({ ...prev, [key]: data }));
      })
      .catch((err: unknown) => {
        const msg = err instanceof Error ? err.message : String(err);
        if (active) setError(`Failed to load room data: ${msg}`);
      })
      .finally(() => {
        if (active) setLoadingRoomDataKey((current) => (current === key ? null : current));
      });

    return () => {
      active = false;
    };
  }, [period, roomDataByKey, selectedRoomId]);

  useEffect(() => {
    const departmentIds = departments
      .map((department) => department.id)
      .filter((id): id is number => id !== undefined);
    if (departmentIds.length === 0) return;

    let active = true;

    const hydrateSlowly = async () => {
      const hydratedRooms: RoomDTO[] = [];

      for (let i = 0; i < departmentIds.length; i++) {
        if (!active) return;

        const departmentId = departmentIds[i];
        const department = departments.find((candidate) => candidate.id === departmentId);
        setHydrationStatus({
          phase: "rooms",
          label: `Loading rooms for ${department?.name ?? `Department ${departmentId}`}`,
          done: i,
          total: departmentIds.length,
        });

        if (!cachedRoomsByDepartment.has(departmentId)) {
          await delay(650);
        }

        const rooms = await getRoomsCached(departmentId);
        if (!active) return;

        hydratedRooms.push(...rooms);
        setRoomsByDepartment((prev) => ({ ...prev, [departmentId]: rooms }));
        setHydrationStatus({
          phase: "rooms",
          label: `Loaded rooms for ${department?.name ?? `Department ${departmentId}`}`,
          done: i + 1,
          total: departmentIds.length,
        });
      }

      const roomsWithIds = hydratedRooms.filter((room): room is RoomDTO & { id: number } => room.id !== undefined);
      for (let i = 0; i < roomsWithIds.length; i++) {
        if (!active) return;

        const room = roomsWithIds[i];
        const key = roomDataKey(room.id, period);
        setHydrationStatus({
          phase: "roomData",
          label: `Loading ${period === "week" ? "week" : "month"} data for ${room.name ?? `Room ${room.id}`}`,
          done: i,
          total: roomsWithIds.length,
        });

        if (!cachedRoomDataByPeriod.has(key)) {
          await delay(900);
        }

        const data = await getRoomDataCached(room.id, period);
        if (!active) return;

        setRoomDataByKey((prev) => ({ ...prev, [key]: data }));
        setHydrationStatus({
          phase: "roomData",
          label: `Loaded ${room.name ?? `Room ${room.id}`}`,
          done: i + 1,
          total: roomsWithIds.length,
        });
      }

      if (active) {
        setHydrationStatus({
          phase: "complete",
          label: `Background loading complete for ${period === "week" ? "week" : "month"}`,
          done: roomsWithIds.length,
          total: roomsWithIds.length,
        });
      }
    };

    void hydrateSlowly().catch((err: unknown) => {
      const msg = err instanceof Error ? err.message : String(err);
      if (active) setError(`Background loading failed: ${msg}`);
    });

    return () => {
      active = false;
    };
  }, [departments, period]);

  const toggleDepartment = (departmentId?: number) => {
    if (departmentId === undefined) return;

    setExpandedDepartmentIds((prev) => {
      const next = new Set(prev);
      if (next.has(departmentId)) {
        next.delete(departmentId);
      } else {
        next.add(departmentId);
        if (!roomsByDepartment[departmentId]) {
          void loadDepartmentRooms(departmentId);
        }
      }
      return next;
    });
  };

  const loadDepartmentRooms = async (departmentId: number) => {
    if (roomsByDepartment[departmentId]) return;

    setLoadingRooms((prev) => new Set(prev).add(departmentId));
    try {
      const rooms = await getRoomsCached(departmentId);
      setRoomsByDepartment((prev) => ({ ...prev, [departmentId]: rooms }));
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : String(err);
      setError(`Failed to load rooms: ${msg}`);
    } finally {
      setLoadingRooms((prev) => {
        const next = new Set(prev);
        next.delete(departmentId);
        return next;
      });
    }
  };

  const selectRoom = (roomId?: number) => {
    if (roomId === undefined) return;
    setSelectedRoomId(roomId);
  };

  const selectedRoomDataKey = selectedRoomId == null ? null : roomDataKey(selectedRoomId, period);
  const selectedRoomData = selectedRoomDataKey ? roomDataByKey[selectedRoomDataKey] : undefined;
  const loadedRoomsCount = useMemo(
    () => Object.values(roomsByDepartment).reduce((sum, rooms) => sum + rooms.length, 0),
    [roomsByDepartment],
  );
  const loadedViolationCount = useMemo(
    () => Object.entries(roomDataByKey)
      .filter(([key]) => key.endsWith(`:${period}`))
      .reduce((sum, [, data]) => sum + data.violations.length, 0),
    [period, roomDataByKey],
  );

  if (loadingDepartments) {
    return (
      <div>
        <NavbarComponent />
        <div style={{ display: "flex", alignItems: "center", justifyContent: "center", minHeight: "60vh" }}>
          <ProgressSpinner />
        </div>
      </div>
    );
  }

  return (
    <div>
      <NavbarComponent />

      <div style={{ padding: "1.5rem 2rem 0" }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: "1rem" }}>
          <div>
            <h2 style={{ margin: "0 0 0.25rem", color: "#111827" }}>
              Welcome{currentUser?.firstName ? `, ${currentUser.firstName}` : ""}
            </h2>
            <p style={{ margin: 0, color: "#6b7280", fontSize: "0.95rem" }}>
              <i className="pi pi-chart-bar" style={{ marginRight: "0.4rem" }} />
              Management Overview
            </p>
          </div>

          <div style={{ display: "flex", gap: "0.4rem" }}>
            {(["week", "month"] as Period[]).map((p) => (
              <button
                key={p}
                onClick={() => setPeriod(p)}
                style={{
                  padding: "0.4rem 1rem",
                  border: "1px solid",
                  borderColor: period === p ? "#0369a1" : "#d1d5db",
                  borderRadius: "6px",
                  background: period === p ? "#0369a1" : "#fff",
                  color: period === p ? "#fff" : "#374151",
                  fontWeight: period === p ? 600 : 400,
                  cursor: "pointer",
                  fontSize: "0.85rem",
                  transition: "all 0.15s",
                }}
              >
                {p === "week" ? "Week" : "Month"}
              </button>
            ))}
          </div>
        </div>

        {error && <Message severity="error" text={error} style={{ marginBottom: "1rem" }} />}
        {hydrationStatus && (
          <div
            style={{
              display: "flex",
              alignItems: "center",
              justifyContent: "space-between",
              gap: "1rem",
              background: hydrationStatus.phase === "complete" ? "#f0fdf4" : "#eff6ff",
              border: `1px solid ${hydrationStatus.phase === "complete" ? "#bbf7d0" : "#bfdbfe"}`,
              color: hydrationStatus.phase === "complete" ? "#166534" : "#1e3a8a",
              borderRadius: "8px",
              padding: "0.65rem 0.9rem",
              marginBottom: "1rem",
              fontSize: "0.9rem",
            }}
          >
            <div style={{ display: "flex", alignItems: "center", gap: "0.55rem" }}>
              {hydrationStatus.phase === "complete" ? (
                <i className="pi pi-check-circle" />
              ) : (
                <ProgressSpinner style={{ width: "1rem", height: "1rem" }} strokeWidth="8" />
              )}
              <span>{hydrationStatus.label}</span>
            </div>
            <span style={{ fontVariantNumeric: "tabular-nums", whiteSpace: "nowrap" }}>
              {hydrationStatus.total > 0 ? `${hydrationStatus.done}/${hydrationStatus.total}` : ""}
            </span>
          </div>
        )}

        <div style={{ display: "flex", gap: "1rem", marginBottom: "1.5rem", flexWrap: "wrap" }}>
          {[
            { icon: "pi-sitemap", label: "Departments", value: departments.length, color: "#0369a1" },
            { icon: "pi-building", label: "Loaded Rooms", value: loadedRoomsCount, color: "#374151" },
            {
              icon: "pi-exclamation-triangle",
              label: "Loaded Violations",
              value: loadedViolationCount,
              color: loadedViolationCount > 0 ? "#ef4444" : "#22c55e",
            },
          ].map((s) => (
            <div
              key={s.label}
              style={{
                display: "flex",
                alignItems: "center",
                gap: "0.6rem",
                background: "#f9fafb",
                border: "1px solid #e5e7eb",
                borderRadius: "8px",
                padding: "0.5rem 1.1rem",
              }}
            >
              <i className={`pi ${s.icon}`} style={{ color: s.color, fontSize: "1.15rem" }} />
              <div>
                <div style={{ fontWeight: 700, fontSize: "1.15rem", color: "#111827", lineHeight: 1 }}>{s.value}</div>
                <div style={{ fontSize: "0.75rem", color: "#6b7280" }}>{s.label}</div>
              </div>
            </div>
          ))}
        </div>

        <div style={{ borderBottom: "2px solid #e5e7eb" }}>
          <div
            style={{
              display: "inline-flex",
              alignItems: "center",
              gap: "0.4rem",
              padding: "0.6rem 1.25rem",
              borderBottom: "2px solid #0369a1",
              marginBottom: "-2px",
              fontWeight: 600,
              color: "#0369a1",
              fontSize: "0.95rem",
            }}
          >
            <i className="pi pi-sitemap" style={{ fontSize: "0.9rem" }} />
            All Departments
            <span style={{ fontSize: "0.8rem", fontWeight: 400, color: "#9ca3af", marginLeft: "0.3rem" }}>
              ({departments.length})
            </span>
          </div>
        </div>
      </div>

      <div
        style={{
          padding: "2rem",
          display: "grid",
          gridTemplateColumns: "repeat(auto-fill, minmax(400px, 1fr))",
          gap: "1.5rem",
        }}
      >
        {departments.map((dept) => {
          const departmentId = dept.id;
          const expanded = departmentId !== undefined && expandedDepartmentIds.has(departmentId);
          const rooms = departmentId !== undefined ? roomsByDepartment[departmentId] ?? [] : [];
          const loadingDeptRooms = departmentId !== undefined && loadingRooms.has(departmentId);

          return (
            <div
              key={dept.id}
              style={{
                background: "#fff",
                border: "1px solid #e5e7eb",
                borderRadius: "12px",
                overflow: "hidden",
                boxShadow: "0 1px 3px rgba(0,0,0,0.05)",
              }}
            >
              <div style={{ height: "4px", background: "#0369a1" }} />

              <div style={{ padding: "1.25rem" }}>
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    marginBottom: "0.85rem",
                    paddingBottom: "0.75rem",
                    borderBottom: "1px solid #f3f4f6",
                  }}
                >
                  <div style={{ display: "flex", alignItems: "center", gap: "0.6rem" }}>
                    <i className="pi pi-sitemap" style={{ color: "#0369a1", fontSize: "1.05rem" }} />
                    <span style={{ fontWeight: 700, fontSize: "1.05rem", color: "#111827" }}>{dept.name ?? "—"}</span>
                  </div>
                  <button
                    type="button"
                    onClick={() => toggleDepartment(departmentId)}
                    disabled={departmentId === undefined}
                    aria-label={expanded ? "Collapse department" : "Expand department"}
                    style={{
                      border: "1px solid #d1d5db",
                      background: "#fff",
                      borderRadius: "6px",
                      cursor: departmentId === undefined ? "not-allowed" : "pointer",
                      padding: "0.35rem 0.55rem",
                    }}
                  >
                    <i className={`pi ${expanded ? "pi-chevron-up" : "pi-chevron-down"}`} />
                  </button>
                </div>

                <div style={{ display: "flex", gap: "0.5rem", marginBottom: "1rem", flexWrap: "wrap" }}>
                  <span
                    style={{
                      background: "#f3f4f6",
                      borderRadius: "20px",
                      padding: "0.2rem 0.7rem",
                      fontSize: "0.8rem",
                      color: "#374151",
                      display: "flex",
                      alignItems: "center",
                      gap: "4px",
                    }}
                  >
                    <i className="pi pi-building" style={{ fontSize: "0.75rem" }} />
                    {rooms.length > 0 ? `${rooms.length} room${rooms.length !== 1 ? "s" : ""}` : "Rooms not loaded"}
                  </span>
                </div>

                {expanded && (
                  <div style={{ marginBottom: "1.25rem" }}>
                    <div
                      style={{
                        fontSize: "0.72rem",
                        fontWeight: 600,
                        color: "#9ca3af",
                        textTransform: "uppercase",
                        letterSpacing: "0.5px",
                        marginBottom: "0.4rem",
                      }}
                    >
                      Rooms
                    </div>
                    {loadingDeptRooms ? (
                      <div style={{ display: "flex", justifyContent: "center", padding: "1rem" }}>
                        <ProgressSpinner style={{ width: "2rem", height: "2rem" }} />
                      </div>
                    ) : rooms.length > 0 ? (
                      <div style={{ display: "flex", flexDirection: "column", gap: "0.3rem" }}>
                        {rooms.map((room) => {
                          const key = room.id === undefined ? null : roomDataKey(room.id, period);
                          const roomData = key ? roomDataByKey[key] : undefined;
                          const violationCount = roomData?.violations.length ?? 0;
                          const selected = room.id !== undefined && selectedRoomId === room.id;

                          return (
                            <button
                              key={room.id}
                              type="button"
                              onClick={() => selectRoom(room.id)}
                              style={{
                                display: "flex",
                                justifyContent: "space-between",
                                alignItems: "center",
                                padding: "0.45rem 0.6rem",
                                borderRadius: "6px",
                                background: selected ? "#eff6ff" : "#f9fafb",
                                border: selected ? "1px solid #93c5fd" : "1px solid transparent",
                                cursor: "pointer",
                                textAlign: "left",
                              }}
                            >
                              <div style={{ display: "flex", alignItems: "center", gap: "0.35rem" }}>
                                {room.privacyMode && <i className="pi pi-lock" style={{ fontSize: "0.7rem", color: "#6b7280" }} />}
                                <span style={{ fontSize: "0.85rem", color: "#374151" }}>{room.name ?? "—"}</span>
                              </div>
                              {roomData ? (
                                <div
                                  title={violationCount === 0 ? "No violations" : `${violationCount} violation(s)`}
                                  style={{
                                    width: "10px",
                                    height: "10px",
                                    borderRadius: "50%",
                                    background: violationColor(violationCount),
                                    flexShrink: 0,
                                  }}
                                />
                              ) : (
                                <span style={{ fontSize: "0.72rem", color: "#9ca3af" }}>Load</span>
                              )}
                            </button>
                          );
                        })}
                      </div>
                    ) : (
                      <NoDataOverlay height="52px" message="No rooms" icon="pi pi-ban" />
                    )}
                  </div>
                )}

                {expanded && selectedRoomId !== null && rooms.some((room) => room.id === selectedRoomId) && (
                  <div style={{ borderTop: "1px solid #f3f4f6", paddingTop: "1rem", display: "flex", flexDirection: "column", gap: "0.65rem" }}>
                    <div style={{ fontSize: "0.72rem", fontWeight: 600, color: "#9ca3af", textTransform: "uppercase", letterSpacing: "0.5px" }}>
                      {period === "week" ? "Last 7 Days" : "Last 30 Days"} - Selected Room Avg. per Day
                    </div>
                    {loadingRoomDataKey === selectedRoomDataKey && (
                      <div style={{ display: "flex", justifyContent: "center", padding: "1rem" }}>
                        <ProgressSpinner style={{ width: "2rem", height: "2rem" }} />
                      </div>
                    )}
                    {selectedRoomData && METRICS.map(({ key, label, unit, color }) =>
                      selectedRoomData.trends[key] && selectedRoomData.trends[key]!.length > 0 ? (
                        <Sparkline
                          key={key}
                          uid={`${selectedRoomId}-${period}`}
                          data={selectedRoomData.trends[key]!}
                          color={color}
                          label={label}
                          unit={unit}
                        />
                      ) : (
                        <div key={key} style={{ display: "flex", alignItems: "center", gap: "0.6rem" }}>
                          <span style={{ fontSize: "0.7rem", color: "#9ca3af", minWidth: "60px", flexShrink: 0 }}>{label}</span>
                          <NoDataOverlay
                            height="36px"
                            message={selectedRoomData.hasGaps[key] ? "Keine Daten" : "Keine Daten"}
                            icon="pi pi-ban"
                          />
                        </div>
                      ),
                    )}
                    {selectedRoomData && selectedRoomData.violations.length > 0 && (
                      <Message
                        severity="warn"
                        text={`${selectedRoomData.violations.length} active threshold violation${selectedRoomData.violations.length > 1 ? "s" : ""} in selected room.`}
                      />
                    )}
                  </div>
                )}
              </div>
            </div>
          );
        })}

        {departments.length === 0 && (
          <div
            style={{
              gridColumn: "1 / -1",
              padding: "3rem",
              textAlign: "center",
              background: "#f9fafb",
              borderRadius: "8px",
              border: "1px dashed #d1d5db",
            }}
          >
            <i className="pi pi-inbox" style={{ fontSize: "2rem", color: "#d1d5db", display: "block", marginBottom: "0.5rem" }} />
            <p style={{ color: "#6b7280", margin: 0 }}>No departments found.</p>
          </div>
        )}
      </div>
    </div>
  );
};

export default ManagementDashboard;

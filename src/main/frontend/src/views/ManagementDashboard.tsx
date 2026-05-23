import React, { useEffect, useState } from "react";
import { ProgressSpinner } from "primereact/progressspinner";
import { Message } from "primereact/message";
import { Badge } from "primereact/badge";
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

interface TrendPoint {
  label: string;
  value: number;
}

interface DeptData {
  dept: DepartmentDTO;
  rooms: RoomDTO[];
  violations: ThresholdViolationDTO[];
  trends: Partial<Record<MeasurementDTOMetricEnum, TrendPoint[]>>;
  hasGaps: Partial<Record<MeasurementDTOMetricEnum, boolean>>;
}

const METRICS: {
  key: MeasurementDTOMetricEnum;
  label: string;
  unit: string;
  color: string;
}[] = [
  {
    key: MeasurementDTOMetricEnum.TEMPERATURE,
    label: "Temperature",
    unit: "°C",
    color: "#f59e0b",
  },
  {
    key: MeasurementDTOMetricEnum.HUMIDITY,
    label: "Humidity",
    unit: "%",
    color: "#0ea5e9",
  },
  {
    key: MeasurementDTOMetricEnum.IAQ,
    label: "Air Quality",
    unit: "IAQ",
    color: "#8b5cf6",
  },
];

function violationColor(count: number): string {
  if (count === 0) return "#22c55e";
  if (count <= 2) return "#f59e0b";
  return "#ef4444";
}

//measures grouped in daily -> average
function bucketByDay(
  measurements: MeasurementDTO[],
  days: number,
): TrendPoint[] {
  const now = Date.now();
  const buckets: Record<string, number[]> = {};

  for (let i = days - 1; i >= 0; i--) {
    const d = new Date(now - i * 86_400_000);
    const key = d.toLocaleDateString("en-GB", {
      day: "2-digit",
      month: "2-digit",
    });
    buckets[key] = [];
  }

  measurements.forEach((m) => {
    if (m.measurement === undefined || !m.timestamp) return;
    const ts = new Date(m.timestamp).getTime();
    if (ts < now - days * 86_400_000) return;
    const key = new Date(m.timestamp).toLocaleDateString("en-GB", {
      day: "2-digit",
      month: "2-digit",
    });
    if (buckets[key] !== undefined) buckets[key].push(m.measurement);
  });

  return Object.entries(buckets)
    .map(([label, vals]) => ({
      label,
      value:
        vals.length > 0 ? vals.reduce((a, b) => a + b, 0) / vals.length : NaN,
    }))
    .filter((p) => !isNaN(p.value));
}

//sparkline
interface SparklineProps {
  uid: string;
  data: TrendPoint[];
  color: string;
  label: string;
  unit: string;
}

const Sparkline: React.FC<SparklineProps> = ({
  uid,
  data,
  color,
  label,
  unit,
}) => {
  const W = 110;
  const H = 38;
  const PAD = 3;

  const values = data.map((d) => d.value);
  const min = Math.min(...values);
  const max = Math.max(...values);
  const range = max - min || 1;
  const latest = values[values.length - 1];

  const toX = (i: number) =>
    PAD + (i / Math.max(values.length - 1, 1)) * (W - 2 * PAD);
  const toY = (v: number) => H - PAD - ((v - min) / range) * (H - 2 * PAD);

  const linePoints = values
    .map((v, i) => `${toX(i).toFixed(1)},${toY(v).toFixed(1)}`)
    .join(" ");
  const areaPoints = [
    ...values.map((v, i) => `${toX(i).toFixed(1)},${toY(v).toFixed(1)}`),
    `${toX(values.length - 1).toFixed(1)},${H}`,
    `${PAD},${H}`,
  ].join(" ");

  const gradId = `spark-${uid}-${label.replace(/\s/g, "")}`;

  return (
    <div style={{ display: "flex", alignItems: "center", gap: "0.6rem" }}>
      <div>
        <div
          style={{ fontSize: "0.7rem", color: "#9ca3af", marginBottom: "2px" }}
        >
          {label}
        </div>
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
              <polyline
                points={linePoints}
                fill="none"
                stroke={color}
                strokeWidth="1.5"
                strokeLinejoin="round"
                strokeLinecap="round"
              />
            </>
          )}
          {values.length === 1 && (
            <circle cx={W / 2} cy={H / 2} r={3} fill={color} />
          )}
        </svg>
      </div>
      <div style={{ minWidth: "42px" }}>
        <div
          style={{
            fontWeight: 700,
            fontSize: "0.95rem",
            color: "#111827",
            lineHeight: 1,
          }}
        >
          {latest.toFixed(1)}
        </div>
        <div style={{ fontSize: "0.7rem", color: "#9ca3af" }}>{unit}</div>
      </div>
    </div>
  );
};

const ManagementDashboard: React.FC = () => {
  const { currentUser } = useUser();

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [deptData, setDeptData] = useState<DeptData[]>([]);
  const [period, setPeriod] = useState<Period>("week");

  useEffect(() => {
    setLoading(true);
    setError(null);

    const load = async () => {
      try {
        const days = period === "week" ? 7 : 30;
        const from = new Date(Date.now() - days * 86_400_000).toISOString();

        const [allDepts, allViolations] = await Promise.all([
          DepartmentService.getAll(),
          ViolationService.getAll({
            violationStatus: ViolationStatusEnum.ACTIVE,
          }),
        ]);

        const results = await Promise.all(
          allDepts.map(async (dept): Promise<DeptData> => {
            if (!dept.id)
              return { dept, rooms: [], violations: [], trends: {}, hasGaps: {} };

            const rooms = await DepartmentService.getRooms(dept.id);
            const violations = allViolations.filter((v) =>
              rooms.some((r) => r.id === v.roomId),
            );

            const allMeasurements: MeasurementDTO[] = [];
            await Promise.all(
              rooms.map((r) =>
                r.id
                  ? MeasurementService.getAll({ roomId: r.id, from }).then(
                      (ms) => allMeasurements.push(...ms),
                    )
                  : Promise.resolve(),
              ),
            );

            const trends: Partial<Record<MeasurementDTOMetricEnum, TrendPoint[]>> = {};
            const hasGaps: Partial<Record<MeasurementDTOMetricEnum, boolean>> = {};
            const fromDate = new Date(Date.now() - days * 86_400_000);
            const toDate = new Date();
            METRICS.forEach(({ key }) => {
              const filtered = allMeasurements.filter((m) => m.metric === key);
              const bucketed = bucketByDay(filtered, days);
              if (bucketed.length > 0) trends[key] = bucketed;
              hasGaps[key] = hasDataGap(filtered, fromDate, toDate);
            });

            return { dept, rooms, violations, trends, hasGaps };
          }),
        );

        setDeptData(results);
      } catch (err: unknown) {
        const msg = err instanceof Error ? err.message : String(err);
        console.error("Management dashboard load error:", err);
        setError(`Failed to load data: ${msg}`);
      } finally {
        setLoading(false);
      }
    };

    void load();
  }, [period]);

  const totalViolations = deptData.reduce((s, d) => s + d.violations.length, 0);
  const totalRooms = deptData.reduce((s, d) => s + d.rooms.length, 0);

  if (loading) {
    return (
      <div>
        <NavbarComponent />
        <div
          className="flex justify-content-center align-items-center"
          style={{ minHeight: "60vh" }}
        >
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

      <div style={{ padding: "1.5rem 2rem 0" }}>
        {/*Header row*/}
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "flex-start",
            marginBottom: "1rem",
          }}
        >
          <div>
            <h2 style={{ margin: "0 0 0.25rem", color: "#111827" }}>
              Welcome
              {currentUser?.firstName ? `, ${currentUser.firstName}` : ""}
            </h2>
            <p style={{ margin: 0, color: "#6b7280", fontSize: "0.95rem" }}>
              <i
                className="pi pi-chart-bar"
                style={{ marginRight: "0.4rem" }}
              />
              Management Overview
            </p>
          </div>

          {/*Period toggle*/}
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

        {/* Global violation banner */}
        {totalViolations > 0 && (
          <div style={{ marginBottom: "1rem" }}>
            <Message
              severity="warn"
              text={`${totalViolations} active threshold violation${totalViolations > 1 ? "s" : ""} across all departments.`}
            />
          </div>
        )}

        {/* Summary chips */}
        <div
          style={{
            display: "flex",
            gap: "1rem",
            marginBottom: "1.5rem",
            flexWrap: "wrap",
          }}
        >
          {[
            {
              icon: "pi-sitemap",
              label: "Departments",
              value: deptData.length,
              color: "#0369a1",
            },
            {
              icon: "pi-building",
              label: "Rooms",
              value: totalRooms,
              color: "#374151",
            },
            {
              icon: "pi-exclamation-triangle",
              label: "Active Violations",
              value: totalViolations,
              color: totalViolations > 0 ? "#ef4444" : "#22c55e",
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
              <i
                className={`pi ${s.icon}`}
                style={{ color: s.color, fontSize: "1.15rem" }}
              />
              <div>
                <div
                  style={{
                    fontWeight: 700,
                    fontSize: "1.15rem",
                    color: "#111827",
                    lineHeight: 1,
                  }}
                >
                  {s.value}
                </div>
                <div style={{ fontSize: "0.75rem", color: "#6b7280" }}>
                  {s.label}
                </div>
              </div>
            </div>
          ))}
        </div>

        {/* Tab strip */}
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
            <span
              style={{
                fontSize: "0.8rem",
                fontWeight: 400,
                color: "#9ca3af",
                marginLeft: "0.3rem",
              }}
            >
              ({deptData.length})
            </span>
          </div>
        </div>
      </div>

      {/* Department card */}
      <div
        style={{
          padding: "2rem",
          display: "grid",
          gridTemplateColumns: "repeat(auto-fill, minmax(400px, 1fr))",
          gap: "1.5rem",
        }}
      >
        {deptData.map(({ dept, rooms, violations, trends, hasGaps }) => {
          const violCount = violations.length;
          const statusColor = violationColor(violCount);

          return (
            <div
              key={dept.id}
              style={{
                background: "#fff",
                border: "1px solid #e5e7eb",
                borderRadius: "12px",
                overflow: "hidden",
                boxShadow: "0 1px 3px rgba(0,0,0,0.05)",
                transition: "box-shadow 0.2s",
              }}
              onMouseEnter={(e) => {
                const el = e.currentTarget as HTMLElement;
                el.style.boxShadow = "0 8px 24px rgba(0,0,0,0.1)";
              }}
              onMouseLeave={(e) => {
                const el = e.currentTarget as HTMLElement;
                el.style.boxShadow = "0 1px 3px rgba(0,0,0,0.05)";
              }}
            >
              {/* Coloured status strip on top */}
              <div style={{ height: "4px", background: statusColor }} />

              <div style={{ padding: "1.25rem" }}>
                {/* Department header */}
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
                  <div
                    style={{
                      display: "flex",
                      alignItems: "center",
                      gap: "0.6rem",
                    }}
                  >
                    <i
                      className="pi pi-sitemap"
                      style={{ color: "#0369a1", fontSize: "1.05rem" }}
                    />
                    <span
                      style={{
                        fontWeight: 700,
                        fontSize: "1.05rem",
                        color: "#111827",
                      }}
                    >
                      {dept.name ?? "—"}
                    </span>
                  </div>
                  {violCount > 0 ? (
                    <span
                      style={{
                        display: "flex",
                        alignItems: "center",
                        gap: "5px",
                        color: statusColor,
                      }}
                    >
                      <i
                        className="pi pi-exclamation-triangle"
                        style={{ fontSize: "1rem" }}
                      />
                      <Badge value={violCount} severity="danger" />
                    </span>
                  ) : (
                    <i
                      className="pi pi-check-circle"
                      style={{ color: "#22c55e", fontSize: "1.15rem" }}
                    />
                  )}
                </div>

                {/* Status pills */}
                <div
                  style={{
                    display: "flex",
                    gap: "0.5rem",
                    marginBottom: "1rem",
                    flexWrap: "wrap",
                  }}
                >
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
                    <i
                      className="pi pi-building"
                      style={{ fontSize: "0.75rem" }}
                    />
                    {rooms.length} room{rooms.length !== 1 ? "s" : ""}
                  </span>
                  <span
                    style={{
                      background: violCount === 0 ? "#f0fdf4" : "#fef2f2",
                      borderRadius: "20px",
                      padding: "0.2rem 0.7rem",
                      fontSize: "0.8rem",
                      color: statusColor,
                      display: "flex",
                      alignItems: "center",
                      gap: "4px",
                    }}
                  >
                    <i
                      className="pi pi-circle-fill"
                      style={{ fontSize: "0.45rem" }}
                    />
                    {violCount === 0
                      ? "All clear"
                      : `${violCount} violation${violCount > 1 ? "s" : ""}`}
                  </span>
                </div>

                {/* Room list with traffic-light dots */}
                {rooms.length > 0 && (
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
                    <div
                      style={{
                        display: "flex",
                        flexDirection: "column",
                        gap: "0.3rem",
                      }}
                    >
                      {rooms.map((r) => {
                        const rViol = violations.filter(
                          (v) => v.roomId === r.id,
                        ).length;
                        return (
                          <div
                            key={r.id}
                            style={{
                              display: "flex",
                              justifyContent: "space-between",
                              alignItems: "center",
                              padding: "0.3rem 0.6rem",
                              borderRadius: "6px",
                              background: "#f9fafb",
                            }}
                          >
                            <div style={{ display: "flex", alignItems: "center", gap: "0.35rem" }}>
                              {r.privacyMode && (
                                <i className="pi pi-lock" style={{ fontSize: "0.7rem", color: "#6b7280" }} />
                              )}
                              <span style={{ fontSize: "0.85rem", color: "#374151" }}>
                                {r.name ?? "—"}
                              </span>
                            </div>
                            <div
                              title={
                                rViol === 0
                                  ? "No violations"
                                  : `${rViol} violation(s)`
                              }
                              style={{
                                width: "10px",
                                height: "10px",
                                borderRadius: "50%",
                                background: violationColor(rViol),
                                flexShrink: 0,
                              }}
                            />
                          </div>
                        );
                      })}
                    </div>
                  </div>
                )}

                {/* Trend sparklines */}
                <div
                  style={{
                    borderTop: "1px solid #f3f4f6",
                    paddingTop: "1rem",
                    display: "flex",
                    flexDirection: "column",
                    gap: "0.65rem",
                  }}
                >
                  <div
                    style={{
                      fontSize: "0.72rem",
                      fontWeight: 600,
                      color: "#9ca3af",
                      textTransform: "uppercase",
                      letterSpacing: "0.5px",
                    }}
                  >
                    {period === "week" ? "Last 7 Days" : "Last 30 Days"} — Avg.
                    per Day
                  </div>
                  {METRICS.map(({ key, label, unit, color }) =>
                    trends[key] && trends[key]!.length > 0 ? (
                      <Sparkline
                        key={key}
                        uid={String(dept.id)}
                        data={trends[key]!}
                        color={color}
                        label={label}
                        unit={unit}
                      />
                    ) : (
                      <div
                        key={key}
                        style={{ display: "flex", alignItems: "center", gap: "0.6rem" }}
                      >
                        <span style={{ fontSize: "0.7rem", color: "#9ca3af", minWidth: "60px", flexShrink: 0 }}>
                          {label}
                        </span>
                        <NoDataOverlay
                          height="36px"
                          message={hasGaps[key] && rooms.some(r => r.privacyMode) ? "Datenschutz aktiv" : "Keine Daten"}
                          icon={hasGaps[key] && rooms.some(r => r.privacyMode) ? "pi pi-lock" : "pi pi-ban"}
                        />
                      </div>
                    ),
                  )}
                </div>

                {/* Footer link hint */}
                <div
                  style={{
                    display: "flex",
                    justifyContent: "flex-end",
                    marginTop: "0.75rem",
                    color: "#9ca3af",
                    fontSize: "0.8rem",
                    gap: "0.4rem",
                    alignItems: "center",
                  }}
                >
                  <span>View department</span>
                  <i
                    className="pi pi-arrow-circle-right"
                    style={{ fontSize: "0.8rem" }}
                  />
                </div>
              </div>
            </div>
          );
        })}

        {deptData.length === 0 && (
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
            <i
              className="pi pi-inbox"
              style={{
                fontSize: "2rem",
                color: "#d1d5db",
                display: "block",
                marginBottom: "0.5rem",
              }}
            />
            <p style={{ color: "#6b7280", margin: 0 }}>No departments found.</p>
          </div>
        )}
      </div>
    </div>
  );
};

export default ManagementDashboard;

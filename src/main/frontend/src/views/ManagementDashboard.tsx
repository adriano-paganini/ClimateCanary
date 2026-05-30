import React, { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { ProgressSpinner } from "primereact/progressspinner";
import { Message } from "primereact/message";
import { Dialog } from "primereact/dialog";
import { Button } from "primereact/button";
import "primeicons/primeicons.css";

import NavbarComponent from "../components/NavbarComponent";
import NoDataOverlay from "../components/NoDataOverlay";
import { useUser } from "../Contexts/AuthenticatedUserContext";
import {
  AnalyticsService,
  ManagementClimateDashboardDTO,
  ManagementClimateTrendDTO,
  ManagementDepartmentClimateDTO,
  ViolationBreakdownDTO,
} from "../services/AnalyticsService";
import { DepartmentService } from "../services/DepartmentService";
import { MeasurementDTOMetricEnum, RoomDTO } from "../generated-skeleton-api";
import { ROUTES } from "../utilities/routes.paths";
import { registerDashboardCacheClearHandler } from "../utilities/dashboardCacheInvalidation";

const METRIC_LABELS: Record<string, string> = {
  [MeasurementDTOMetricEnum.TEMPERATURE]: "Temperature",
  [MeasurementDTOMetricEnum.HUMIDITY]: "Humidity",
  [MeasurementDTOMetricEnum.IAQ]: "Air Quality",
  [MeasurementDTOMetricEnum.PRESSURE]: "Pressure",
};

const DIRECTION_META: Record<string, { label: string; icon: string; color: string; background: string; border: string }> = {
  IMPROVED: {
    label: "Improved",
    icon: "pi-arrow-up-right",
    color: "#166534",
    background: "#f0fdf4",
    border: "#bbf7d0",
  },
  WORSENED: {
    label: "Worsened",
    icon: "pi-arrow-down-right",
    color: "#991b1b",
    background: "#fef2f2",
    border: "#fecaca",
  },
  UNCHANGED: {
    label: "Unchanged",
    icon: "pi-minus",
    color: "#374151",
    background: "#f9fafb",
    border: "#e5e7eb",
  },
  NO_DATA: {
    label: "No data",
    icon: "pi-ban",
    color: "#6b7280",
    background: "#f9fafb",
    border: "#e5e7eb",
  },
};

let cachedDashboard: ManagementClimateDashboardDTO | null = null;
let cachedDashboardRequest: Promise<ManagementClimateDashboardDTO> | null = null;

function clearManagementDashboardCache(): void {
  cachedDashboard = null;
  cachedDashboardRequest = null;
}

registerDashboardCacheClearHandler(clearManagementDashboardCache);

async function getDashboardCached(): Promise<ManagementClimateDashboardDTO> {
  if (cachedDashboard !== null) return cachedDashboard;
  if (cachedDashboardRequest === null) {
    cachedDashboardRequest = AnalyticsService.getManagementClimateDashboard()
      .then((dashboard) => {
        cachedDashboard = dashboard;
        return dashboard;
      })
      .finally(() => {
        cachedDashboardRequest = null;
      });
  }
  return cachedDashboardRequest;
}

function DirectionBadge({ direction }: { direction?: string }) {
  const meta = DIRECTION_META[direction ?? "NO_DATA"] ?? DIRECTION_META.NO_DATA;

  return (
    <span
      style={{
        display: "inline-flex",
        alignItems: "center",
        gap: "0.35rem",
        minWidth: "7rem",
        justifyContent: "center",
        padding: "0.35rem 0.55rem",
        borderRadius: "6px",
        border: `1px solid ${meta.border}`,
        background: meta.background,
        color: meta.color,
        fontSize: "0.82rem",
        fontWeight: 650,
      }}
    >
      <i className={`pi ${meta.icon}`} style={{ fontSize: "0.78rem" }} />
      {meta.label}
    </span>
  );
}

function WarningPill({
  warning,
  onClick,
}: {
  warning: ViolationBreakdownDTO;
  onClick?: (warning: ViolationBreakdownDTO) => void;
}) {
  return (
    <span
      role={onClick ? "button" : undefined}
      tabIndex={onClick ? 0 : undefined}
      onClick={onClick ? () => onClick(warning) : undefined}
      onKeyDown={onClick ? (e) => { if (e.key === "Enter" || e.key === " ") onClick(warning); } : undefined}
      style={{
        display: "inline-flex",
        alignItems: "center",
        gap: "0.35rem",
        padding: "0.25rem 0.55rem",
        borderRadius: "6px",
        background: "#fff7ed",
        border: "1px solid #fed7aa",
        color: "#9a3412",
        fontSize: "0.8rem",
        fontWeight: 600,
        cursor: onClick ? "pointer" : "default",
        userSelect: "none",
        transition: "background 0.15s",
      }}
    >
      <i className="pi pi-exclamation-triangle" style={{ fontSize: "0.72rem" }} />
      {METRIC_LABELS[warning.label ?? ""] ?? warning.label}: {warning.count ?? 0}
    </span>
  );
}

function WarningCountBadge({ count }: { count: number }) {
  const hasWarnings = count > 0;

  return (
    <span
      style={{
        display: "inline-flex",
        alignItems: "center",
        gap: "0.35rem",
        padding: "0.35rem 0.6rem",
        borderRadius: "6px",
        border: `1px solid ${hasWarnings ? "#fed7aa" : "#bbf7d0"}`,
        background: hasWarnings ? "#fff7ed" : "#f0fdf4",
        color: hasWarnings ? "#9a3412" : "#166534",
        fontSize: "0.82rem",
        fontWeight: 650,
        whiteSpace: "nowrap",
      }}
    >
      <i className={`pi ${hasWarnings ? "pi-exclamation-triangle" : "pi-check-circle"}`} style={{ fontSize: "0.76rem" }} />
      {count} warning{count === 1 ? "" : "s"}
    </span>
  );
}

function TrendRow({ trend }: { trend: ManagementClimateTrendDTO }) {
  const label = METRIC_LABELS[trend.metric ?? ""] ?? trend.metric ?? "Unknown";

  return (
    <div
      style={{
        display: "grid",
        gridTemplateColumns: "minmax(7rem, 1fr) auto auto",
        alignItems: "center",
        gap: "0.6rem",
        padding: "0.55rem 0",
        borderBottom: "1px solid #f3f4f6",
      }}
    >
      <span style={{ color: "#374151", fontWeight: 600, fontSize: "0.9rem" }}>{label}</span>
      <DirectionBadge direction={trend.weeklyDirection} />
      <DirectionBadge direction={trend.monthlyDirection} />
    </div>
  );
}

const ManagementDashboard: React.FC = () => {
  const { currentUser } = useUser();
  const navigate = useNavigate();
  const [dashboard, setDashboard] = useState<ManagementClimateDashboardDTO | null>(cachedDashboard);
  const [loading, setLoading] = useState(cachedDashboard === null);
  const [error, setError] = useState<string | null>(null);
  const [filterWarningsOnly, setFilterWarningsOnly] = useState(false);
  const [detailDept, setDetailDept] = useState<ManagementDepartmentClimateDTO | null>(null);
  const [detailRooms, setDetailRooms] = useState<RoomDTO[]>([]);
  const [detailRoomsLoading, setDetailRoomsLoading] = useState(false);
  const [detailRoomsError, setDetailRoomsError] = useState<string | null>(null);
  const deptGridRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    let active = true;
    setLoading(cachedDashboard === null);
    setError(null);

    getDashboardCached()
      .then((data) => {
        if (active) setDashboard(data);
      })
      .catch((err: unknown) => {
        const msg = err instanceof Error ? err.message : String(err);
        if (active) setError(`Failed to load management climate overview: ${msg}`);
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, []);

  const departments = dashboard?.departments ?? [];
  const warningDepartments = useMemo(
    () => departments.filter((department) => (department.activeWarnings ?? 0) > 0).length,
    [departments],
  );
  const visibleDepartments = filterWarningsOnly
    ? departments.filter((d) => (d.activeWarnings ?? 0) > 0)
    : departments;

  function scrollToDepts() {
    deptGridRef.current?.scrollIntoView({ behavior: "smooth", block: "start" });
  }

  function handleDeptStatClick() {
    setFilterWarningsOnly(false);
    setTimeout(scrollToDepts, 50);
  }

  function handleViolationsStatClick() {
    setFilterWarningsOnly(true);
    setTimeout(scrollToDepts, 50);
  }

  function handleAffectedStatClick() {
    setFilterWarningsOnly((prev) => !prev);
    setTimeout(scrollToDepts, 50);
  }

  function openDeptDetail(dept: ManagementDepartmentClimateDTO) {
    setDetailDept(dept);
    setDetailRooms([]);
    setDetailRoomsError(null);
    if (dept.departmentId !== undefined) {
      setDetailRoomsLoading(true);
      DepartmentService.getRooms(dept.departmentId)
        .then((rooms) => setDetailRooms(rooms))
        .catch(() => setDetailRoomsError("Could not load rooms for this department."))
        .finally(() => setDetailRoomsLoading(false));
    }
  }

  function closeDeptDetail() {
    setDetailDept(null);
    setDetailRooms([]);
    setDetailRoomsError(null);
  }

  if (loading) {
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

      <main style={{ padding: "1.5rem 2rem 2rem" }}>
        <div style={{ display: "flex", justifyContent: "space-between", gap: "1rem", alignItems: "flex-start", marginBottom: "1rem" }}>
          <div>
            <h2 style={{ margin: "0 0 0.25rem", color: "#111827" }}>
              Welcome{currentUser?.firstName ? `, ${currentUser.firstName}` : ""}
            </h2>
            <p style={{ margin: 0, color: "#6b7280", fontSize: "0.95rem" }}>
              <i className="pi pi-chart-bar" style={{ marginRight: "0.4rem" }} />
              Department climate changes and warnings
            </p>
          </div>
          <div style={{ display: "flex", gap: "0.75rem", flexWrap: "wrap", justifyContent: "flex-end" }}>
            <SummaryStat
              icon="pi-sitemap"
              label="Departments"
              value={departments.length}
              color="#0369a1"
              onClick={handleDeptStatClick}
              tooltip="Show all departments"
            />
            <SummaryStat
              icon="pi-exclamation-triangle"
              label="Active Violations"
              value={dashboard?.totalActiveWarnings ?? 0}
              color={(dashboard?.totalActiveWarnings ?? 0) > 0 ? "#dc2626" : "#16a34a"}
              onClick={handleViolationsStatClick}
              active={filterWarningsOnly}
              tooltip="Filter departments with active violations"
            />
            <SummaryStat
              icon="pi-building"
              label="Departments affected"
              value={warningDepartments}
              color={warningDepartments > 0 ? "#f59e0b" : "#16a34a"}
              onClick={handleAffectedStatClick}
              active={filterWarningsOnly}
              tooltip="Toggle filter: departments with warnings"
            />
          </div>
        </div>

        {filterWarningsOnly && (
          <div
            style={{
              display: "flex",
              alignItems: "center",
              gap: "0.75rem",
              marginBottom: "1rem",
              padding: "0.5rem 0.75rem",
              background: "#fff7ed",
              border: "1px solid #fed7aa",
              borderRadius: "8px",
            }}
          >
            <i className="pi pi-filter" style={{ color: "#ea580c" }} />
            <span style={{ fontSize: "0.9rem", color: "#9a3412", flex: 1 }}>
              Showing only departments with active violations ({visibleDepartments.length} of {departments.length})
            </span>
            <Button
              icon="pi pi-times"
              label="Show all"
              className="p-button-text p-button-sm"
              style={{ color: "#9a3412", padding: "0.25rem 0.5rem" }}
              onClick={() => setFilterWarningsOnly(false)}
            />
          </div>
        )}

        {error && <Message severity="error" text={error} style={{ marginBottom: "1rem" }} />}

        <div
          ref={deptGridRef}
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fill, minmax(420px, 1fr))",
            gap: "1.25rem",
            scrollMarginTop: "1.5rem",
          }}
        >
          {visibleDepartments.map((department) => (
            <section
              key={department.departmentId}
              style={{
                background: "#fff",
                border: "1px solid #e5e7eb",
                borderRadius: "8px",
                overflow: "hidden",
                boxShadow: "0 1px 3px rgba(0,0,0,0.05)",
              }}
            >
              <div style={{ height: "4px", background: (department.activeWarnings ?? 0) > 0 ? "#f59e0b" : "#16a34a" }} />
              <div style={{ padding: "1.15rem" }}>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", gap: "1rem", marginBottom: "1rem" }}>
                  <div style={{ minWidth: 0 }}>
                    <div style={{ display: "flex", alignItems: "center", gap: "0.55rem", marginBottom: "0.2rem" }}>
                      <i className="pi pi-sitemap" style={{ color: "#0369a1" }} />
                      <h3 style={{ margin: 0, color: "#111827", fontSize: "1.05rem" }}>{department.departmentName ?? "Department"}</h3>
                    </div>
                    <span style={{ color: "#6b7280", fontSize: "0.82rem" }}>Week and month compared with previous equal period</span>
                  </div>
                  <WarningCountBadge count={department.activeWarnings ?? 0} />
                </div>

                <div style={{ display: "flex", gap: "0.5rem", flexWrap: "wrap", minHeight: "2rem", marginBottom: "1rem" }}>
                  {(department.warningsByMetric ?? []).length > 0 ? (
                    department.warningsByMetric?.map((warning) => (
                      <WarningPill
                        key={warning.label}
                        warning={warning}
                        onClick={() => openDeptDetail(department)}
                      />
                    ))
                  ) : (
                    <span style={{ color: "#166534", fontSize: "0.85rem", fontWeight: 600 }}>
                      <i className="pi pi-check-circle" style={{ marginRight: "0.35rem" }} />
                      No active department warnings
                    </span>
                  )}
                </div>

                <div
                  style={{
                    display: "grid",
                    gridTemplateColumns: "minmax(7rem, 1fr) auto auto",
                    gap: "0.6rem",
                    color: "#6b7280",
                    fontSize: "0.74rem",
                    fontWeight: 700,
                    textTransform: "uppercase",
                    letterSpacing: "0.04em",
                    paddingBottom: "0.4rem",
                    borderBottom: "1px solid #e5e7eb",
                  }}
                >
                  <span>Statistic</span>
                  <span style={{ textAlign: "center" }}>Last week</span>
                  <span style={{ textAlign: "center" }}>Last month</span>
                </div>

                {(department.trends ?? []).map((trend) => <TrendRow key={trend.metric} trend={trend} />)}

                <div style={{ marginTop: "1rem", display: "flex", justifyContent: "flex-end" }}>
                  <Button
                    label="View Department"
                    icon="pi pi-arrow-right"
                    iconPos="right"
                    className="p-button-outlined"
                    style={{ borderColor: "#0369a1", color: "#0369a1" }}
                    onClick={() => openDeptDetail(department)}
                  />
                </div>
              </div>
            </section>
          ))}

          {visibleDepartments.length === 0 && (
            <div style={{ gridColumn: "1 / -1" }}>
              <NoDataOverlay
                height="220px"
                message={
                  filterWarningsOnly
                    ? "No departments with active violations"
                    : "No department climate overview available"
                }
                icon={filterWarningsOnly ? "pi pi-check-circle" : "pi pi-inbox"}
              />
            </div>
          )}
        </div>
      </main>

      {/* Department detail dialog */}
      <Dialog
        header={
          <div style={{ display: "flex", alignItems: "center", gap: "0.6rem" }}>
            <i className="pi pi-sitemap" style={{ color: "#0369a1" }} />
            <span>{detailDept?.departmentName ?? "Department"}</span>
          </div>
        }
        visible={detailDept !== null}
        onHide={closeDeptDetail}
        style={{ width: "520px", maxWidth: "95vw" }}
        modal
        draggable={false}
      >
        {detailDept && (
          <div>
            <div style={{ marginBottom: "1.25rem" }}>
              <div style={{ fontSize: "0.8rem", color: "#6b7280", fontWeight: 700, textTransform: "uppercase", letterSpacing: "0.04em", marginBottom: "0.5rem" }}>
                Active Violations
              </div>
              {(detailDept.warningsByMetric ?? []).length > 0 ? (
                <div style={{ display: "flex", gap: "0.5rem", flexWrap: "wrap" }}>
                  {detailDept.warningsByMetric?.map((warning) => (
                    <WarningPill key={warning.label} warning={warning} />
                  ))}
                </div>
              ) : (
                <span style={{ color: "#166534", fontSize: "0.9rem", fontWeight: 600 }}>
                  <i className="pi pi-check-circle" style={{ marginRight: "0.4rem" }} />
                  No active violations
                </span>
              )}
            </div>

            <div>
              <div style={{ fontSize: "0.8rem", color: "#6b7280", fontWeight: 700, textTransform: "uppercase", letterSpacing: "0.04em", marginBottom: "0.5rem" }}>
                Rooms
              </div>
              {detailRoomsLoading ? (
                <div style={{ display: "flex", alignItems: "center", gap: "0.5rem", color: "#6b7280" }}>
                  <ProgressSpinner style={{ width: "20px", height: "20px" }} />
                  <span style={{ fontSize: "0.9rem" }}>Loading rooms…</span>
                </div>
              ) : detailRoomsError ? (
                <Message severity="warn" text={detailRoomsError} />
              ) : detailRooms.length === 0 ? (
                <span style={{ color: "#6b7280", fontSize: "0.9rem" }}>No rooms found.</span>
              ) : (
                <ul style={{ margin: 0, padding: 0, listStyle: "none", display: "flex", flexDirection: "column", gap: "0.4rem" }}>
                  {detailRooms.map((room) => (
                    <li
                      key={room.id}
                      role="button"
                      tabIndex={0}
                      onClick={() => room.id !== undefined && navigate(ROUTES.MANAGEMENT_ROOM_HISTORY.replace(":roomId", String(room.id)))}
                      onKeyDown={(e) => { if ((e.key === "Enter" || e.key === " ") && room.id !== undefined) navigate(ROUTES.MANAGEMENT_ROOM_HISTORY.replace(":roomId", String(room.id))); }}
                      style={{
                        display: "flex",
                        alignItems: "center",
                        gap: "0.5rem",
                        padding: "0.4rem 0.6rem",
                        background: "#f9fafb",
                        borderRadius: "6px",
                        border: "1px solid #e5e7eb",
                        fontSize: "0.9rem",
                        color: "#374151",
                        cursor: "pointer",
                        userSelect: "none",
                      }}
                    >
                      <i className="pi pi-building" style={{ color: "#6b7280", fontSize: "0.85rem" }} />
                      <span style={{ flex: 1 }}>{room.name ?? `Room ${room.id}`}</span>
                      <i className="pi pi-chart-line" style={{ color: "#9ca3af", fontSize: "0.8rem" }} />
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </div>
        )}
      </Dialog>
    </div>
  );
};

function SummaryStat({
  icon,
  label,
  value,
  color,
  onClick,
  active,
  tooltip,
}: {
  icon: string;
  label: string;
  value: number;
  color: string;
  onClick?: () => void;
  active?: boolean;
  tooltip?: string;
}) {
  const [hovered, setHovered] = useState(false);

  return (
    <div
      role={onClick ? "button" : undefined}
      tabIndex={onClick ? 0 : undefined}
      title={tooltip}
      onClick={onClick}
      onKeyDown={onClick ? (e) => { if (e.key === "Enter" || e.key === " ") onClick(); } : undefined}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      style={{
        display: "flex",
        alignItems: "center",
        gap: "0.6rem",
        background: active ? "#eff6ff" : hovered && onClick ? "#f3f4f6" : "#f9fafb",
        border: `1px solid ${active ? "#bfdbfe" : hovered && onClick ? "#d1d5db" : "#e5e7eb"}`,
        borderRadius: "8px",
        padding: "0.5rem 1rem",
        cursor: onClick ? "pointer" : undefined,
        transition: "background 0.15s, border-color 0.15s",
        userSelect: "none",
      }}
    >
      <i className={`pi ${icon}`} style={{ color, fontSize: "1.1rem" }} />
      <div>
        <div style={{ fontWeight: 750, fontSize: "1.1rem", color: "#111827", lineHeight: 1 }}>{value}</div>
        <div style={{ fontSize: "0.75rem", color: "#6b7280", whiteSpace: "nowrap" }}>{label}</div>
      </div>
    </div>
  );
}

export default ManagementDashboard;

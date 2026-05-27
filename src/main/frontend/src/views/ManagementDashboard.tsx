import React, { useEffect, useMemo, useState } from "react";
import { ProgressSpinner } from "primereact/progressspinner";
import { Message } from "primereact/message";
import "primeicons/primeicons.css";

import NavbarComponent from "../components/NavbarComponent";
import NoDataOverlay from "../components/NoDataOverlay";
import { useUser } from "../Contexts/AuthenticatedUserContext";
import {
  AnalyticsService,
  ManagementClimateDashboardDTO,
  ManagementClimateTrendDTO,
  ViolationBreakdownDTO,
} from "../services/AnalyticsService";
import { MeasurementDTOMetricEnum } from "../generated-skeleton-api";
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

function WarningPill({ warning }: { warning: ViolationBreakdownDTO }) {
  return (
    <span
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
  const [dashboard, setDashboard] = useState<ManagementClimateDashboardDTO | null>(cachedDashboard);
  const [loading, setLoading] = useState(cachedDashboard === null);
  const [error, setError] = useState<string | null>(null);

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
            <SummaryStat icon="pi-sitemap" label="Departments" value={departments.length} color="#0369a1" />
            <SummaryStat icon="pi-exclamation-triangle" label="Warnings" value={dashboard?.totalActiveWarnings ?? 0} color={(dashboard?.totalActiveWarnings ?? 0) > 0 ? "#dc2626" : "#16a34a"} />
            <SummaryStat icon="pi-building" label="Departments affected" value={warningDepartments} color={warningDepartments > 0 ? "#f59e0b" : "#16a34a"} />
          </div>
        </div>

        {error && <Message severity="error" text={error} style={{ marginBottom: "1rem" }} />}

        <div
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fill, minmax(420px, 1fr))",
            gap: "1.25rem",
          }}
        >
          {departments.map((department) => (
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
                    department.warningsByMetric?.map((warning) => <WarningPill key={warning.label} warning={warning} />)
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
              </div>
            </section>
          ))}

          {departments.length === 0 && (
            <div style={{ gridColumn: "1 / -1" }}>
              <NoDataOverlay height="220px" message="No department climate overview available" icon="pi pi-inbox" />
            </div>
          )}
        </div>
      </main>
    </div>
  );
};

function SummaryStat({ icon, label, value, color }: { icon: string; label: string; value: number; color: string }) {
  return (
    <div
      style={{
        display: "flex",
        alignItems: "center",
        gap: "0.6rem",
        background: "#f9fafb",
        border: "1px solid #e5e7eb",
        borderRadius: "8px",
        padding: "0.5rem 1rem",
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

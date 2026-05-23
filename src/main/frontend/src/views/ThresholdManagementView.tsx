import "../styles/App.css";
import "primereact/resources/themes/lara-light-cyan/theme.css";
import "primeicons/primeicons.css";
import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Button } from "primereact/button";
import { Card } from "primereact/card";
import { Column } from "primereact/column";
import { ConfirmDialog, confirmDialog } from "primereact/confirmdialog";
import { DataTable } from "primereact/datatable";
import { Dialog } from "primereact/dialog";
import { Dropdown } from "primereact/dropdown";
import { InputNumber } from "primereact/inputnumber";
import { InputSwitch } from "primereact/inputswitch";
import { InputText } from "primereact/inputtext";
import { MultiSelect } from "primereact/multiselect";
import { TabPanel, TabView } from "primereact/tabview";
import { Tag } from "primereact/tag";
import { Toast } from "primereact/toast";

import NavbarComponent from "../components/NavbarComponent";
import {
  ClimateHintCreateDTO,
  ClimateHintCreateDTOMetricEnum,
  ClimateHintDTO,
  ClimateHintUpdateDTO,
  RoomDTO,
  ThresholdCreateDTO,
  ThresholdCreateDTOMetricEnum,
  ThresholdCreateDTOThresholdTypeEnum,
  ThresholdDTO,
  ThresholdUpdateDTO,
} from "../generated-skeleton-api";
import { ClimateHintService } from "../services/ClimateHintService";
import { RoomService } from "../services/RoomService";
import { ThresholdService } from "../services/ThresholdService";

type MetricSeverity =
  | "success"
  | "info"
  | "warning"
  | "danger"
  | "secondary"
  | "contrast"
  | null
  | undefined;

const metricSeverity = (metric?: string): MetricSeverity => {
  switch (metric) {
    case "TEMPERATURE":
      return "danger";
    case "HUMIDITY":
      return "info";
    case "PRESSURE":
      return "warning";
    case "IAQ":
      return "success";
    default:
      return "secondary";
  }
};

const metricOptions = Object.values(ThresholdCreateDTOMetricEnum).map((v) => ({
  label: v,
  value: v,
}));
const thresholdTypeOptions = Object.values(
  ThresholdCreateDTOThresholdTypeEnum,
).map((v) => ({ label: v, value: v }));
const climateHintMetricOptions = Object.values(
  ClimateHintCreateDTOMetricEnum,
).map((v) => ({ label: v, value: v }));

const emptyThresholdForm = (): Partial<ThresholdCreateDTO> & {
  enabled: boolean;
} => ({
  roomId: undefined,
  metric: undefined,
  boundValue: undefined,
  thresholdType: undefined,
  climateHintIds: [],
  enabled: true,
});

const emptyHintForm = (): Partial<ClimateHintCreateDTO> => ({
  metric: undefined,
  hintText: "",
});

const ThresholdManagementView: React.FC = () => {
  const [thresholds, setThresholds] = useState<ThresholdDTO[]>([]);
  const [climateHints, setClimateHints] = useState<ClimateHintDTO[]>([]);
  const [rooms, setRooms] = useState<RoomDTO[]>([]);
  const [loadingThresholds, setLoadingThresholds] = useState(true);
  const [loadingHints, setLoadingHints] = useState(true);
  const [hintSearch, setHintSearch] = useState("");
  const [collapsedThresholdGroups, setCollapsedThresholdGroups] = useState<Record<string, boolean>>({});
  const [collapsedHintGroups, setCollapsedHintGroups] = useState<Record<string, boolean>>({});

  const [thresholdDialogVisible, setThresholdDialogVisible] = useState(false);
  const [editingThreshold, setEditingThreshold] = useState<ThresholdDTO | null>(
    null,
  );
  const [isNewThreshold, setIsNewThreshold] = useState(false);
  const [thresholdForm, setThresholdForm] = useState(emptyThresholdForm());

  const [hintDialogVisible, setHintDialogVisible] = useState(false);
  const [editingHint, setEditingHint] = useState<ClimateHintDTO | null>(null);
  const [isNewHint, setIsNewHint] = useState(false);
  const [hintForm, setHintForm] = useState(emptyHintForm());

  const toast = useRef<Toast | null>(null);

  const loadThresholds = useCallback(async () => {
    try {
      const data = await ThresholdService.getAll();
      setThresholds(data);
    } catch {
      toast.current?.show({
        severity: "error",
        summary: "Error",
        detail: "Failed to load thresholds",
        life: 3000,
      });
    } finally {
      setLoadingThresholds(false);
    }
  }, []);

  const loadClimateHints = useCallback(async () => {
    try {
      const data = await ClimateHintService.getAll();
      setClimateHints(data);
    } catch {
      toast.current?.show({
        severity: "error",
        summary: "Error",
        detail: "Failed to load climate hints",
        life: 3000,
      });
    } finally {
      setLoadingHints(false);
    }
  }, []);

  const loadRooms = useCallback(async () => {
    try {
      const data = await RoomService.getAll();
      setRooms(data);
    } catch {
      console.error("Failed to load rooms");
    }
  }, []);

  useEffect(() => {
    void loadThresholds();
    void loadClimateHints();
    void loadRooms();
  }, [loadThresholds, loadClimateHints, loadRooms]);

  const getRoomName = (roomId?: number) => {
    if (!roomId) return "—";
    const room = rooms.find((r) => r.id === roomId);
    return room?.name ?? `Room ${roomId}`;
  };

  //threshold crud
  const openNewThreshold = () => {
    setThresholdForm(emptyThresholdForm());
    setEditingThreshold(null);
    setIsNewThreshold(true);
    setThresholdDialogVisible(true);
  };

  const openEditThreshold = (threshold: ThresholdDTO) => {
    setThresholdForm({
      roomId: threshold.roomId,
      metric: threshold.metric as ThresholdCreateDTOMetricEnum | undefined,
      boundValue: threshold.boundValue,
      thresholdType: threshold.thresholdType as
        | ThresholdCreateDTOThresholdTypeEnum
        | undefined,
      climateHintIds: threshold.climateHintIds ?? [],
      enabled: threshold.enabled ?? true,
    });
    setEditingThreshold(threshold);
    setIsNewThreshold(false);
    setThresholdDialogVisible(true);
  };

  const saveThreshold = async () => {
    if (
      !thresholdForm.roomId ||
      !thresholdForm.metric ||
      thresholdForm.boundValue === undefined ||
      !thresholdForm.thresholdType
    ) {
      toast.current?.show({
        severity: "warn",
        summary: "Validation",
        detail: "Please fill in all required fields",
        life: 3000,
      });
      return;
    }
    try {
      if (isNewThreshold) {
        const dto: ThresholdCreateDTO = {
          roomId: thresholdForm.roomId,
          metric: thresholdForm.metric,
          boundValue: thresholdForm.boundValue,
          thresholdType: thresholdForm.thresholdType,
          climateHintIds: thresholdForm.climateHintIds,
        };
        const created = await ThresholdService.create(dto);
        setThresholds((prev) => [...prev, created]);
        toast.current?.show({
          severity: "success",
          summary: "Created",
          detail: "Threshold created",
          life: 3000,
        });
      } else if (editingThreshold?.id !== undefined) {
        const dto: ThresholdUpdateDTO = {
          roomId: thresholdForm.roomId,
          metric: thresholdForm.metric,
          boundValue: thresholdForm.boundValue,
          thresholdType: thresholdForm.thresholdType,
          climateHintIds: thresholdForm.climateHintIds,
          enabled: thresholdForm.enabled,
        };
        const updated = await ThresholdService.update(editingThreshold.id, dto);
        setThresholds((prev) =>
          prev.map((t) => (t.id === updated.id ? updated : t)),
        );
        toast.current?.show({
          severity: "success",
          summary: "Updated",
          detail: "Threshold updated",
          life: 3000,
        });
      }
      setThresholdDialogVisible(false);
    } catch {
      toast.current?.show({
        severity: "error",
        summary: "Error",
        detail: "Failed to save threshold",
        life: 3000,
      });
    }
  };

  const confirmDeleteThreshold = (threshold: ThresholdDTO) => {
    confirmDialog({
      message: `Delete this ${threshold.metric} threshold for room "${getRoomName(threshold.roomId)}"?`,
      header: "Confirm Delete",
      icon: "pi pi-exclamation-triangle",
      acceptClassName: "p-button-danger",
      accept: () => deleteThreshold(threshold),
    });
  };

  const deleteThreshold = async (threshold: ThresholdDTO) => {
    if (!threshold.id) return;
    try {
      await ThresholdService.delete(threshold.id);
      setThresholds((prev) => prev.filter((t) => t.id !== threshold.id));
      toast.current?.show({
        severity: "success",
        summary: "Deleted",
        detail: "Threshold deleted",
        life: 3000,
      });
    } catch {
      toast.current?.show({
        severity: "error",
        summary: "Error",
        detail: "Failed to delete threshold",
        life: 3000,
      });
    }
  };

  const toggleThresholdEnabled = async (threshold: ThresholdDTO) => {
    if (!threshold.id) return;
    try {
      const updated = await ThresholdService.update(threshold.id, {
        enabled: !threshold.enabled,
      });
      setThresholds((prev) =>
        prev.map((t) => (t.id === updated.id ? updated : t)),
      );
    } catch {
      toast.current?.show({
        severity: "error",
        summary: "Error",
        detail: "Failed to update threshold",
        life: 3000,
      });
    }
  };

  //climaate hint
  const openNewHint = () => {
    setHintForm(emptyHintForm());
    setEditingHint(null);
    setIsNewHint(true);
    setHintDialogVisible(true);
  };

  const openEditHint = (hint: ClimateHintDTO) => {
    setHintForm({
      metric: hint.metric as ClimateHintCreateDTOMetricEnum | undefined,
      hintText: hint.hintText ?? "",
    });
    setEditingHint(hint);
    setIsNewHint(false);
    setHintDialogVisible(true);
  };

  const saveHint = async () => {
    if (!hintForm.metric || !hintForm.hintText?.trim()) {
      toast.current?.show({
        severity: "warn",
        summary: "Validation",
        detail: "Please fill in all required fields",
        life: 3000,
      });
      return;
    }
    try {
      if (isNewHint) {
        const dto: ClimateHintCreateDTO = {
          metric: hintForm.metric,
          hintText: hintForm.hintText,
        };
        const created = await ClimateHintService.create(dto);
        setClimateHints((prev) => [...prev, created]);
        toast.current?.show({
          severity: "success",
          summary: "Created",
          detail: "Climate hint created",
          life: 3000,
        });
      } else if (editingHint?.id !== undefined) {
        const dto: ClimateHintUpdateDTO = {
          metric: hintForm.metric,
          hintText: hintForm.hintText,
        };
        const updated = await ClimateHintService.update(editingHint.id, dto);
        setClimateHints((prev) =>
          prev.map((h) => (h.id === updated.id ? updated : h)),
        );
        toast.current?.show({
          severity: "success",
          summary: "Updated",
          detail: "Climate hint updated",
          life: 3000,
        });
      }
      setHintDialogVisible(false);
    } catch {
      toast.current?.show({
        severity: "error",
        summary: "Error",
        detail: "Failed to save climate hint",
        life: 3000,
      });
    }
  };

  const confirmDeleteHint = (hint: ClimateHintDTO) => {
    confirmDialog({
      message: `Delete climate hint "${hint.hintText}"?`,
      header: "Confirm Delete",
      icon: "pi pi-exclamation-triangle",
      acceptClassName: "p-button-danger",
      accept: () => deleteHint(hint),
    });
  };

  const deleteHint = async (hint: ClimateHintDTO) => {
    if (!hint.id) return;
    try {
      await ClimateHintService.delete(hint.id);
      setClimateHints((prev) => prev.filter((h) => h.id !== hint.id));
      toast.current?.show({
        severity: "success",
        summary: "Deleted",
        detail: "Climate hint deleted",
        life: 3000,
      });
    } catch {
      toast.current?.show({
        severity: "error",
        summary: "Error",
        detail: "Failed to delete climate hint",
        life: 3000,
      });
    }
  };

  //derived state
  const sortedThresholds = useMemo(() => [...thresholds].sort((a, b) => {
    const roomComparison = getRoomName(a.roomId).localeCompare(getRoomName(b.roomId));
    if (roomComparison !== 0) return roomComparison;
    const metricComparison = (a.metric ?? "").localeCompare(b.metric ?? "");
    if (metricComparison !== 0) return metricComparison;
    return (a.thresholdType ?? "").localeCompare(b.thresholdType ?? "");
  }), [thresholds, rooms]);

  const filteredClimateHints = useMemo(() => {
    const query = hintSearch.trim().toLowerCase();
    return climateHints
      .filter((hint) => !query || (hint.hintText ?? "").toLowerCase().includes(query))
      .sort((a, b) => {
        const metricComparison = (a.metric ?? "").localeCompare(b.metric ?? "");
        if (metricComparison !== 0) return metricComparison;
        return (a.hintText ?? "").localeCompare(b.hintText ?? "");
      });
  }, [climateHints, hintSearch]);

  const groupedThresholds = useMemo(() => {
    const roomIds = Array.from(new Set(sortedThresholds.map((threshold) => threshold.roomId ?? -1)));

    return roomIds.map((roomId) => {
      const roomThresholds = sortedThresholds.filter((threshold) => (threshold.roomId ?? -1) === roomId);
      return {
        key: String(roomId),
        label: roomId === -1 ? "Unassigned room" : getRoomName(roomId),
        rows: roomThresholds,
        metricGroups: metricOptions
          .map((option) => ({
            key: String(option.value),
            label: option.label,
            rows: roomThresholds.filter((threshold) => threshold.metric === option.value),
          }))
          .filter((group) => group.rows.length > 0),
      };
    });
  }, [sortedThresholds, rooms]);

  const groupedClimateHints = useMemo(() => climateHintMetricOptions
    .map((option) => ({
      key: String(option.value),
      label: option.label,
      rows: filteredClimateHints.filter((hint) => hint.metric === option.value),
    }))
    .filter((group) => group.rows.length > 0), [filteredClimateHints]);

  const climateHintOptions = climateHints.map((h) => ({
    label: `[${h.metric}] ${h.hintText}`,
    value: h.id,
  }));

  const roomOptions = rooms.map((r) => ({
    label: r.name ?? `Room ${r.id}`,
    value: r.id,
  }));

  const metricTemplate = (
    row: ThresholdDTO, //column
  ) => <Tag value={row.metric} severity={metricSeverity(row.metric)} />;

  const enabledTemplate = (row: ThresholdDTO) => (
    <InputSwitch
      checked={row.enabled ?? false}
      onChange={() => toggleThresholdEnabled(row)}
    />
  );

  const thresholdActionsTemplate = (row: ThresholdDTO) => (
    <div style={{ display: "flex", gap: "0.5rem" }}>
      <Button
        icon="pi pi-pencil"
        rounded
        text
        severity="info"
        aria-label="Edit"
        onClick={() => openEditThreshold(row)}
      />
      <Button
        icon="pi pi-trash"
        rounded
        text
        severity="danger"
        aria-label="Delete"
        onClick={() => confirmDeleteThreshold(row)}
      />
    </div>
  );

  const hintMetricTemplate = (row: ClimateHintDTO) => (
    <Tag value={row.metric} severity={metricSeverity(row.metric)} />
  );

  const hintActionsTemplate = (row: ClimateHintDTO) => (
    <div style={{ display: "flex", gap: "0.5rem" }}>
      <Button
        icon="pi pi-pencil"
        rounded
        text
        severity="info"
        aria-label="Edit"
        onClick={() => openEditHint(row)}
      />
      <Button
        icon="pi pi-trash"
        rounded
        text
        severity="danger"
        aria-label="Delete"
        onClick={() => confirmDeleteHint(row)}
      />
    </div>
  );

  const toggleThresholdGroup = (key: string) => {
    setCollapsedThresholdGroups((prev) => ({ ...prev, [key]: !prev[key] }));
  };

  const toggleHintGroup = (key: string) => {
    setCollapsedHintGroups((prev) => ({ ...prev, [key]: !prev[key] }));
  };

  const thresholdDialogFooter = (
    <div>
      <Button
        label="Cancel"
        icon="pi pi-times"
        text
        onClick={() => setThresholdDialogVisible(false)}
      />
      <Button label="Save" icon="pi pi-check" onClick={saveThreshold} />
    </div>
  );

  const hintDialogFooter = (
    <div>
      <Button
        label="Cancel"
        icon="pi pi-times"
        text
        onClick={() => setHintDialogVisible(false)}
      />
      <Button label="Save" icon="pi pi-check" onClick={saveHint} />
    </div>
  );

  return (
    <div>
      <NavbarComponent />
      <Toast ref={toast} />
      <ConfirmDialog />

      <div className="m-4">
        <TabView>
          <TabPanel header="Thresholds" leftIcon="pi pi-sliders-h mr-2">
            <Card>
              <Button
                label="Add Threshold"
                icon="pi pi-plus"
                className="p-button-raised p-button-rounded"
                style={{ marginBottom: "1rem" }}
                onClick={openNewThreshold}
              />
              {loadingThresholds ? (
                <DataTable value={[]} loading emptyMessage="Loading thresholds..." />
              ) : groupedThresholds.length === 0 ? (
                <div style={{ color: "#64748b", padding: "1rem 0" }}>No thresholds found</div>
              ) : (
                <div style={{ display: "flex", flexDirection: "column", gap: "0.75rem" }}>
                  {groupedThresholds.map((group) => {
                    const collapsed = collapsedThresholdGroups[group.key] ?? false;
                    return (
                      <div key={group.key} style={{ border: "1px solid #e5e7eb", borderRadius: "8px", overflow: "hidden" }}>
                        <button
                          type="button"
                          onClick={() => toggleThresholdGroup(group.key)}
                          style={{
                            width: "100%",
                            border: 0,
                            background: "#f8fafc",
                            padding: "0.75rem 1rem",
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "space-between",
                            cursor: "pointer",
                          }}
                        >
                          <span style={{ display: "inline-flex", alignItems: "center", gap: "0.5rem", fontWeight: 700 }}>
                            <i className={`pi ${collapsed ? "pi-chevron-right" : "pi-chevron-down"}`} />
                            <i className="pi pi-building" />
                            <span>{group.label}</span>
                          </span>
                          <span style={{ color: "#64748b", fontSize: "0.9rem" }}>
                            {group.rows.length} threshold{group.rows.length === 1 ? "" : "s"}
                          </span>
                        </button>
                        {!collapsed && (
                          <div style={{ display: "flex", flexDirection: "column", gap: "0.75rem", padding: "0.75rem" }}>
                            {group.metricGroups.map((metricGroup) => (
                              <div key={metricGroup.key}>
                                <div style={{ display: "flex", alignItems: "center", gap: "0.5rem", marginBottom: "0.5rem" }}>
                                  <Tag value={metricGroup.label} severity={metricSeverity(metricGroup.key)} />
                                  <span style={{ color: "#64748b", fontSize: "0.9rem" }}>
                                    {metricGroup.rows.length} threshold{metricGroup.rows.length === 1 ? "" : "s"}
                                  </span>
                                </div>
                                <DataTable
                                  value={metricGroup.rows}
                                  emptyMessage="No thresholds found"
                                  stripedRows
                                  sortField="thresholdType"
                                  sortOrder={1}
                                >
                                  <Column field="thresholdType" header="Type" sortable />
                                  <Column field="boundValue" header="Limit Value" sortable />
                                  <Column
                                    field="enabled"
                                    header="Enabled"
                                    body={enabledTemplate}
                                  />
                                  <Column
                                    body={thresholdActionsTemplate}
                                    header="Actions"
                                    style={{ width: "8rem" }}
                                  />
                                </DataTable>
                              </div>
                            ))}
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              )}
            </Card>
          </TabPanel>

          <TabPanel header="Climate Hints" leftIcon="pi pi-lightbulb mr-2">
            <Card>
              <div style={{ display: "flex", justifyContent: "space-between", gap: "1rem", marginBottom: "1rem", flexWrap: "wrap" }}>
                <InputText
                  value={hintSearch}
                  onChange={(e) => setHintSearch(e.target.value)}
                  placeholder="Search climate hints by text..."
                  style={{ minWidth: "18rem", flex: "1 1 18rem" }}
                />
                <Button
                  label="Add Climate Hint"
                  icon="pi pi-plus"
                  className="p-button-raised p-button-rounded"
                  onClick={openNewHint}
                />
              </div>
              {loadingHints ? (
                <DataTable value={[]} loading emptyMessage="Loading climate hints..." />
              ) : groupedClimateHints.length === 0 ? (
                <div style={{ color: "#64748b", padding: "1rem 0" }}>No climate hints found</div>
              ) : (
                <div style={{ display: "flex", flexDirection: "column", gap: "0.75rem" }}>
                  {groupedClimateHints.map((group) => {
                    const collapsed = collapsedHintGroups[group.key] ?? false;
                    return (
                      <div key={group.key} style={{ border: "1px solid #e5e7eb", borderRadius: "8px", overflow: "hidden" }}>
                        <button
                          type="button"
                          onClick={() => toggleHintGroup(group.key)}
                          style={{
                            width: "100%",
                            border: 0,
                            background: "#f8fafc",
                            padding: "0.75rem 1rem",
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "space-between",
                            cursor: "pointer",
                          }}
                        >
                          <span style={{ display: "inline-flex", alignItems: "center", gap: "0.5rem", fontWeight: 700 }}>
                            <i className={`pi ${collapsed ? "pi-chevron-right" : "pi-chevron-down"}`} />
                            <Tag value={group.label} severity={metricSeverity(group.key)} />
                          </span>
                          <span style={{ color: "#64748b", fontSize: "0.9rem" }}>
                            {group.rows.length} hint{group.rows.length === 1 ? "" : "s"}
                          </span>
                        </button>
                        {!collapsed && (
                          <DataTable
                            value={group.rows}
                            emptyMessage="No climate hints found"
                            stripedRows
                            sortField="hintText"
                            sortOrder={1}
                          >
                            <Column
                              field="metric"
                              header="Metric"
                              body={hintMetricTemplate}
                              style={{ width: "10rem" }}
                              sortable
                            />
                            <Column field="hintText" header="Hint Text" sortable />
                            <Column
                              body={hintActionsTemplate}
                              header="Actions"
                              style={{ width: "8rem" }}
                            />
                          </DataTable>
                        )}
                      </div>
                    );
                  })}
                </div>
              )}
            </Card>
          </TabPanel>
        </TabView>
      </div>

      {/* Threshold */}
      <Dialog
        visible={thresholdDialogVisible}
        header={isNewThreshold ? "Add Threshold" : "Edit Threshold"}
        onHide={() => setThresholdDialogVisible(false)}
        style={{ width: "500px" }}
        footer={thresholdDialogFooter}
      >
        <div
          style={{
            display: "flex",
            flexDirection: "column",
            gap: "1rem",
            paddingTop: "0.5rem",
          }}
        >
          <div className="field">
            <label htmlFor="t-room">Room *</label>
            <Dropdown
              id="t-room"
              value={thresholdForm.roomId}
              options={roomOptions}
              onChange={(e) =>
                setThresholdForm((f) => ({ ...f, roomId: e.value }))
              }
              placeholder="Select room"
              style={{ width: "100%" }}
              filter
            />
          </div>
          <div className="field">
            <label htmlFor="t-metric">Metric *</label>
            <Dropdown
              id="t-metric"
              value={thresholdForm.metric}
              options={metricOptions}
              onChange={(e) =>
                setThresholdForm((f) => ({ ...f, metric: e.value }))
              }
              placeholder="Select metric"
              style={{ width: "100%" }}
            />
          </div>
          <div className="field">
            <label htmlFor="t-type">Threshold Type *</label>
            <Dropdown
              id="t-type"
              value={thresholdForm.thresholdType}
              options={thresholdTypeOptions}
              onChange={(e) =>
                setThresholdForm((f) => ({ ...f, thresholdType: e.value }))
              }
              placeholder="Select type (LOWER / UPPER)"
              style={{ width: "100%" }}
            />
          </div>
          <div className="field">
            <label htmlFor="t-value">Limit Value *</label>
            <InputNumber
              id="t-value"
              value={thresholdForm.boundValue ?? null}
              onValueChange={(e) =>
                setThresholdForm((f) => ({
                  ...f,
                  boundValue: e.value ?? undefined,
                }))
              }
              placeholder="Enter limit value"
              style={{ width: "100%" }}
              useGrouping={false}
              maxFractionDigits={2}
            />
          </div>
          <div className="field">
            <label htmlFor="t-hints">Climate Hints</label>
            <MultiSelect
              id="t-hints"
              value={thresholdForm.climateHintIds ?? []}
              options={climateHintOptions}
              onChange={(e) =>
                setThresholdForm((f) => ({ ...f, climateHintIds: e.value }))
              }
              placeholder="Select climate hints"
              style={{ width: "100%" }}
              display="chip"
            />
          </div>
          {!isNewThreshold && (
            <div
              className="field"
              style={{ display: "flex", alignItems: "center", gap: "0.75rem" }}
            >
              <label htmlFor="t-enabled">Enabled</label>
              <InputSwitch
                id="t-enabled"
                checked={thresholdForm.enabled ?? true}
                onChange={(e) =>
                  setThresholdForm((f) => ({ ...f, enabled: e.value }))
                }
              />
            </div>
          )}
        </div>
      </Dialog>

      {/* Climate Hint */}
      <Dialog
        visible={hintDialogVisible}
        header={isNewHint ? "Add Climate Hint" : "Edit Climate Hint"}
        onHide={() => setHintDialogVisible(false)}
        style={{ width: "450px" }}
        footer={hintDialogFooter}
      >
        <div
          style={{
            display: "flex",
            flexDirection: "column",
            gap: "1rem",
            paddingTop: "0.5rem",
          }}
        >
          <div className="field">
            <label htmlFor="h-metric">Metric *</label>
            <Dropdown
              id="h-metric"
              value={hintForm.metric}
              options={climateHintMetricOptions}
              onChange={(e) => setHintForm((f) => ({ ...f, metric: e.value }))}
              placeholder="Select metric"
              style={{ width: "100%" }}
            />
          </div>
          <div className="field">
            <label htmlFor="h-text">Hint Text *</label>
            <InputText
              id="h-text"
              value={hintForm.hintText ?? ""}
              onChange={(e) =>
                setHintForm((f) => ({ ...f, hintText: e.target.value }))
              }
              placeholder="Enter climate hint text"
              style={{ width: "100%" }}
            />
          </div>
        </div>
      </Dialog>

    </div>
  );
};

export default ThresholdManagementView;

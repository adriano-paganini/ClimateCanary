import "../styles/App.css";
import "primereact/resources/themes/lara-light-cyan/theme.css";
import "primeicons/primeicons.css";
import React, { useCallback, useEffect, useRef, useState } from "react";
import { Button } from "primereact/button";
import { Card } from "primereact/card";
import { Column } from "primereact/column";
import { ConfirmDialog, confirmDialog } from "primereact/confirmdialog";
import { DataTable } from "primereact/datatable";
import { Dialog } from "primereact/dialog";
import { Dropdown } from "primereact/dropdown";
import { InputText } from "primereact/inputtext";
import { Tag } from "primereact/tag";
import { Toast } from "primereact/toast";

import NavbarComponent from "../components/NavbarComponent";
import {
  BuildingDTO,
  DepartmentDTO,
  RoomCreateDTO,
  RoomDTO,
  RoomType,
  RoomUpdateDTO,
} from "../generated-skeleton-api";
import { BuildingService } from "../services/BuildingService";
import { DepartmentService } from "../services/DepartmentService";
import { RoomService } from "../services/RoomService";

const roomTypeOptions = Object.values(RoomType).map((v) => ({
  label: v.replace("_", " "),
  value: v,
}));

const emptyForm = (): Partial<RoomCreateDTO> => ({
  name: "",
  roomType: undefined,
  departmentId: undefined,
  buildingId: undefined,
});

const RoomManagementView: React.FC = () => {
  const [rooms, setRooms] = useState<RoomDTO[]>([]);
  const [departments, setDepartments] = useState<DepartmentDTO[]>([]);
  const [buildings, setBuildings] = useState<BuildingDTO[]>([]);
  const [loading, setLoading] = useState(true);

  const [dialogVisible, setDialogVisible] = useState(false);
  const [editingRoom, setEditingRoom] = useState<RoomDTO | null>(null);
  const [isNew, setIsNew] = useState(false);
  const [form, setForm] = useState(emptyForm());

  const toast = useRef<Toast | null>(null);

  const loadRooms = useCallback(async () => {
    try {
      const data = await RoomService.getAll();
      setRooms(data);
    } catch {
      toast.current?.show({
        severity: "error",
        summary: "Error",
        detail: "Failed to load rooms",
        life: 3000,
      });
    } finally {
      setLoading(false);
    }
  }, []);

  const loadDepartments = useCallback(async () => {
    try {
      const data = await DepartmentService.getAll();
      setDepartments(data);
    } catch {
      console.error("Failed to load departments");
    }
  }, []);

  const loadBuildings = useCallback(async () => {
    try {
      const data = await BuildingService.getAll();
      setBuildings(data);
    } catch {
      console.error("Failed to load buildings");
    }
  }, []);

  useEffect(() => {
    void loadRooms();
    void loadDepartments();
    void loadBuildings();
  }, [loadRooms, loadDepartments, loadBuildings]);

  const getDepartmentName = (id?: number) => {
    if (!id) return "—";
    return departments.find((d) => d.id === id)?.name ?? `Dept. ${id}`;
  };

  const getBuildingName = (id?: number) => {
    if (!id) return "—";
    return buildings.find((b) => b.id === id)?.name ?? `Building ${id}`;
  };

  const openNew = () => {
    setForm(emptyForm());
    setEditingRoom(null);
    setIsNew(true);
    setDialogVisible(true);
  };

  const openEdit = (room: RoomDTO) => {
    setForm({
      name: room.name ?? "",
      roomType: room.roomType,
      departmentId: room.departmentId,
      buildingId: room.buildingId,
    });
    setEditingRoom(room);
    setIsNew(false);
    setDialogVisible(true);
  };

  const save = async () => {
    if (
      !form.name?.trim() ||
      !form.roomType ||
      form.departmentId === undefined ||
      form.buildingId === undefined
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
      if (isNew) {
        const dto: RoomCreateDTO = {
          name: form.name,
          roomType: form.roomType,
          privacyMode: false,
          departmentId: form.departmentId,
          buildingId: form.buildingId,
        };
        const created = await RoomService.create(dto);
        setRooms((prev) => [...prev, created]);
        toast.current?.show({
          severity: "success",
          summary: "Created",
          detail: "Room created",
          life: 3000,
        });
      } else if (editingRoom?.id !== undefined) {
        const dto: RoomUpdateDTO = {
          name: form.name,
          roomType: form.roomType,
          departmentId: form.departmentId,
          buildingId: form.buildingId,
        };
        const updated = await RoomService.update(editingRoom.id, dto);
        setRooms((prev) => prev.map((r) => (r.id === updated.id ? updated : r)));
        toast.current?.show({
          severity: "success",
          summary: "Updated",
          detail: "Room updated",
          life: 3000,
        });
      }
      setDialogVisible(false);
    } catch {
      toast.current?.show({
        severity: "error",
        summary: "Error",
        detail: "Failed to save room",
        life: 3000,
      });
    }
  };

  const confirmDelete = (room: RoomDTO) => {
    confirmDialog({
      message: `Delete room "${room.name}"? This will deactivate it and decommission all associated Raspberry Pis and Arduinos.`,
      header: "Confirm Delete",
      icon: "pi pi-exclamation-triangle",
      acceptClassName: "p-button-danger",
      accept: () => deleteRoom(room),
    });
  };

  const deleteRoom = async (room: RoomDTO) => {
    if (!room.id) return;
    try {
      await RoomService.delete(room.id);
      setRooms((prev) => prev.filter((r) => r.id !== room.id));
      toast.current?.show({
        severity: "success",
        summary: "Deleted",
        detail: "Room deleted",
        life: 3000,
      });
    } catch {
      toast.current?.show({
        severity: "error",
        summary: "Error",
        detail: "Failed to delete room",
        life: 3000,
      });
    }
  };

  const roomTypeTemplate = (row: RoomDTO) => (
    <Tag
      value={row.roomType?.replace("_", " ")}
      severity={row.roomType === RoomType.OFFICE ? "info" : "warning"}
    />
  );

  const privacyTemplate = (row: RoomDTO) => (
    <Tag
      value={row.privacyMode ? "On" : "Off"}
      severity={row.privacyMode ? "warning" : "success"}
      icon={row.privacyMode ? "pi pi-lock" : "pi pi-lock-open"}
    />
  );

  const activeTemplate = (row: RoomDTO) => (
    <Tag
      value={row.active ? "Active" : "Inactive"}
      severity={row.active ? "success" : "danger"}
    />
  );

  const actionsTemplate = (row: RoomDTO) => (
    <div style={{ display: "flex", gap: "0.5rem" }}>
      <Button
        icon="pi pi-pencil"
        rounded
        text
        severity="info"
        aria-label="Edit"
        onClick={() => openEdit(row)}
      />
      <Button
        icon="pi pi-trash"
        rounded
        text
        severity="danger"
        aria-label="Delete"
        onClick={() => confirmDelete(row)}
      />
    </div>
  );

  const dialogFooter = (
    <div>
      <Button
        label="Cancel"
        icon="pi pi-times"
        text
        onClick={() => setDialogVisible(false)}
      />
      <Button label="Save" icon="pi pi-check" onClick={save} />
    </div>
  );

  const departmentOptions = departments.map((d) => ({
    label: d.name ?? `Dept. ${d.id}`,
    value: d.id,
  }));
  const buildingOptions = buildings.map((b) => ({
    label: b.name ?? `Building ${b.id}`,
    value: b.id,
  }));

  return (
    <div>
      <NavbarComponent />
      <Toast ref={toast} />
      <ConfirmDialog />

      <div className="m-4">
        <Card>
          <Button
            label="Add Room"
            icon="pi pi-plus"
            className="p-button-raised p-button-rounded"
            style={{ marginBottom: "1rem" }}
            onClick={openNew}
          />
          <DataTable
            value={rooms}
            loading={loading}
            emptyMessage="No rooms found"
            stripedRows
            paginator
            rows={25}
            sortField="name"
            sortOrder={1}
          >
            <Column field="name" header="Name" sortable />
            <Column field="roomType" header="Type" body={roomTypeTemplate} sortable />
            <Column
              header="Department"
              body={(row: RoomDTO) => getDepartmentName(row.departmentId)}
              sortable
              sortField="departmentId"
              sortFunction={(e) =>
                [...e.data].sort((a, b) =>
                  (e.order ?? 1) *
                  getDepartmentName(a.departmentId).localeCompare(
                    getDepartmentName(b.departmentId)
                  )
                )
              }
            />
            <Column
              header="Building"
              body={(row: RoomDTO) => getBuildingName(row.buildingId)}
              sortable
              sortField="buildingId"
              sortFunction={(e) =>
                [...e.data].sort((a, b) =>
                  (e.order ?? 1) *
                  getBuildingName(a.buildingId).localeCompare(
                    getBuildingName(b.buildingId)
                  )
                )
              }
            />
            <Column field="privacyMode" header="Privacy Mode" body={privacyTemplate} />
            <Column field="active" header="Status" body={activeTemplate} />
            <Column
              body={actionsTemplate}
              header="Actions"
              style={{ width: "8rem" }}
            />
          </DataTable>
        </Card>
      </div>

      <Dialog
        visible={dialogVisible}
        header={isNew ? "Add Room" : "Edit Room"}
        onHide={() => setDialogVisible(false)}
        style={{ width: "480px" }}
        footer={dialogFooter}
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
            <label htmlFor="r-name">Name *</label>
            <InputText
              id="r-name"
              value={form.name ?? ""}
              onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
              placeholder="Enter room name"
              style={{ width: "100%" }}
            />
          </div>
          <div className="field">
            <label htmlFor="r-type">Room Type *</label>
            <Dropdown
              id="r-type"
              value={form.roomType}
              options={roomTypeOptions}
              onChange={(e) => setForm((f) => ({ ...f, roomType: e.value }))}
              placeholder="Select room type"
              style={{ width: "100%" }}
            />
          </div>
          <div className="field">
            <label htmlFor="r-dept">Department *</label>
            <Dropdown
              id="r-dept"
              value={form.departmentId}
              options={departmentOptions}
              onChange={(e) => setForm((f) => ({ ...f, departmentId: e.value }))}
              placeholder="Select department"
              style={{ width: "100%" }}
              filter
            />
          </div>
          <div className="field">
            <label htmlFor="r-building">Building *</label>
            <Dropdown
              id="r-building"
              value={form.buildingId}
              options={buildingOptions}
              onChange={(e) => setForm((f) => ({ ...f, buildingId: e.value }))}
              placeholder="Select building"
              style={{ width: "100%" }}
              filter
            />
          </div>
        </div>
      </Dialog>

    </div>
  );
};

export default RoomManagementView;

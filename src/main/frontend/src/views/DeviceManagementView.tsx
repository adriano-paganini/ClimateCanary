import '../styles/App.css';
import 'primeicons/primeicons.css';
import React, { useCallback, useEffect, useRef, useState } from 'react';
import { Button } from 'primereact/button';
import { Card } from 'primereact/card';
import { Column } from 'primereact/column';
import { ConfirmDialog, confirmDialog } from 'primereact/confirmdialog';
import { DataTable } from 'primereact/datatable';
import { Dialog } from 'primereact/dialog';
import { Dropdown } from 'primereact/dropdown';
import { InputNumber } from 'primereact/inputnumber';
import { InputText } from 'primereact/inputtext';
import { Message } from 'primereact/message';
import { ProgressSpinner } from 'primereact/progressspinner';
import { Tag } from 'primereact/tag';
import { Toast } from 'primereact/toast';
import NavbarComponent from '../components/NavbarComponent';
import { RaspberryPiService } from '../services/RaspberryPiService';
import { SensorStationService } from '../services/SensorStationService';
import { RoomService } from '../services/RoomService';
import {
    RaspberryPiCreateDTO,
    RaspberryPiDTO,
    RaspberryPiUpdateDTO,
    RoomDTO,
    SensorStationDTO,
    SensorStationUpdateDTO,
    SensorStationUpdateDTODeviceStatusEnum,
} from '../generated-skeleton-api';

type TagSeverity = 'success' | 'warning' | 'danger' | 'secondary' | 'info' | 'contrast' | undefined;

function statusSeverity(status?: string): TagSeverity {
    if (status === 'ONLINE' || status === 'CONNECTED') return 'success';
    if (status === 'DEGRADED' || status === 'AVAILABLE') return 'warning';
    if (status === 'OFFLINE' || status === 'MAINTENANCE' || status === 'CONNECTION_FAILED') return 'danger';
    return 'secondary';
}


interface RpiForm {
    hostName: string;
    roomId: number | null;
}

interface StationForm {
    name: string;
    bleMac: string;
    measurementInterval: number | null;
    roomId: number | null;
    deviceStatus: string;
}

const EMPTY_RPI_FORM: RpiForm = { hostName: '', roomId: null };

const DeviceManagementView: React.FC = () => {
    const toast = useRef<Toast>(null);

    const [pis, setPis] = useState<RaspberryPiDTO[]>([]);
    const [decommissionedPis, setDecommissionedPis] = useState<RaspberryPiDTO[]>([]);
    const [allStations, setAllStations] = useState<SensorStationDTO[]>([]);
    const [rooms, setRooms] = useState<RoomDTO[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    // sensor stations keyed by piId, loaded on row expand
    const [stationsMap, setStationsMap] = useState<Record<number, SensorStationDTO[]>>({});
    const [expandedRows, setExpandedRows] = useState<any>(null);

    const [rpiDialogVisible, setRpiDialogVisible] = useState(false);
    const [rpiIsNew, setRpiIsNew] = useState(true);
    const [selectedPi, setSelectedPi] = useState<RaspberryPiDTO | null>(null);
    const [rpiForm, setRpiForm] = useState<RpiForm>(EMPTY_RPI_FORM);
    const [rpiError, setRpiError] = useState<string | null>(null);

    const [stationDialogVisible, setStationDialogVisible] = useState(false);
    const [stationIsNew, setStationIsNew] = useState(true);
    const [selectedStation, setSelectedStation] = useState<SensorStationDTO | null>(null);
    const [stationPiId, setStationPiId] = useState<number | null>(null);
    const [stationForm, setStationForm] = useState<StationForm>({ name: '', bleMac: '', measurementInterval: null, roomId: null, deviceStatus: '' });
    const [stationError, setStationError] = useState<string | null>(null);

    // ble scan dialog state
    const [scanDialogVisible, setScanDialogVisible] = useState(false);
    const [scanPiId, setScanPiId] = useState<number | null>(null);
    const [scanning, setScanning] = useState(false);
    const [scanTriggered, setScanTriggered] = useState(false);
    const [scanCountdown, setScanCountdown] = useState<number | null>(null);
    const [availableStations, setAvailableStations] = useState<SensorStationDTO[]>([]);

    const fetchPis = useCallback(async () => {
        try {
            setPis(await RaspberryPiService.getAll());
        } catch {
            setError('Failed to load Raspberry Pis. Is the backend running?');
        } finally {
            setLoading(false);
        }
    }, []);

    const fetchDecommissionedPis = useCallback(async () => {
        try {
            setDecommissionedPis(await RaspberryPiService.getAllDecommissioned());
        } catch {
            // non-critical
        }
    }, []);

    const fetchAllStations = useCallback(async () => {
        try {
            setAllStations(await SensorStationService.getAll());
        } catch {
            // non-critical
        }
    }, []);

    const fetchRooms = useCallback(async () => {
        try {
            setRooms(await RoomService.getAll());
        } catch {
            // non-critical; dropdowns will be empty
        }
    }, []);

    useEffect(() => {
        void fetchPis();
        void fetchDecommissionedPis();
        void fetchAllStations();
        void fetchRooms();
    }, [fetchPis, fetchDecommissionedPis, fetchAllStations, fetchRooms]);

    useEffect(() => {
        const id = setInterval(() => void fetchPis(), 30_000);
        return () => clearInterval(id);
    }, [fetchPis]);

    const fetchSensorStations = async (piId: number) => {
        try {
            const data = await RaspberryPiService.getSensorStations(piId);
            setStationsMap(prev => ({ ...prev, [piId]: data }));
        } catch {
            toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Could not load sensor stations', life: 3000 });
        }
    };

    // for downloading conf.yaml from a Raspberry Pi

    const downloadConfig = async (pi: RaspberryPiDTO) => {
        if (pi.id == null) return;
        try {
            const yaml = await RaspberryPiService.getConfig(pi.id);
            const blob = new Blob([yaml], { type: 'application/x-yaml' });
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `conf_${pi.hostName ?? pi.id}.yaml`;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            URL.revokeObjectURL(url);
        } catch {
            toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to download conf.yaml', life: 3000 });
        }
    };

    const openScanDialog = (piId: number) => {
        setScanPiId(piId);
        setAvailableStations([]);
        setScanning(false);
        setScanTriggered(false);
        setScanCountdown(null);
        setScanDialogVisible(true);
    };

    const startScan = async () => {
        if (scanPiId == null) return;
        setScanning(true);
        setScanTriggered(true);
        setScanCountdown(10);
        setAvailableStations([]);
        try {
            await SensorStationService.triggerScan(scanPiId);
        } catch {
            setScanTriggered(false);
            setScanCountdown(null);
            toast.current?.show({ severity: 'error', summary: 'Scan Failed', detail: 'BLE scan failed. Make sure the Raspberry Pi is online.', life: 4000 });
        } finally {
            setScanning(false);
        }
    };

    useEffect(() => {
        if (scanCountdown == null || scanCountdown <= 0) return;
        const id = setTimeout(() => setScanCountdown(c => (c ?? 1) - 1), 1_000);
        return () => clearTimeout(id);
    }, [scanCountdown]);

    useEffect(() => {
        if (!scanTriggered || scanPiId == null) return;
        const id = setInterval(async () => {
            try {
                const found = await SensorStationService.getAvailableForPi(scanPiId);
                if (found.length > 0) {
                    setAvailableStations(found);
                    clearInterval(id);
                    toast.current?.show({
                        severity: 'success',
                        summary: 'Devices Found',
                        detail: `${found.length} Arduino device(s) discovered. Select one to set it up.`,
                        life: 6000,
                    });
                }
            } catch { /* ignore polling errors */ }
        }, 5_000);
        return () => clearInterval(id);
    }, [scanTriggered, scanPiId]);

    const selectAvailableStation = (station: SensorStationDTO) => {
        setScanDialogVisible(false);
        setStationIsNew(false);
        setSelectedStation(station);
        setStationPiId(station.raspberryPiId ?? scanPiId);
        setStationForm({
            name: station.name ?? '',
            bleMac: station.bleMac ?? '',
            measurementInterval: station.measurementInterval ?? null,
            roomId: station.roomId ?? null,
            deviceStatus: station.deviceStatus ?? SensorStationUpdateDTODeviceStatusEnum.AVAILABLE,
        });
        setStationError(null);
        setStationDialogVisible(true);
    };



    const openCreateRpi = () => {
        setRpiIsNew(true);
        setSelectedPi(null);
        setRpiForm(EMPTY_RPI_FORM);
        setRpiError(null);
        setRpiDialogVisible(true);
    };

    const openEditRpi = (pi: RaspberryPiDTO) => {
        setRpiIsNew(false);
        setSelectedPi(pi);
        setRpiForm({
            hostName: pi.hostName ?? '',
            roomId: pi.roomId ?? null,
        });
        setRpiError(null);
        setRpiDialogVisible(true);
    };

    const saveRpi = async () => {
        if (rpiIsNew) {
            if (!rpiForm.hostName.trim()) { setRpiError('Host Name is required'); return; }
            if (!rpiForm.roomId) { setRpiError('Room is required'); return; }
            try {
                const dto: RaspberryPiCreateDTO = { hostName: rpiForm.hostName.trim(), roomId: rpiForm.roomId };
                await RaspberryPiService.create(dto);
                setPis(await RaspberryPiService.getAll());
                setRpiDialogVisible(false);
                toast.current?.show({ severity: 'success', summary: 'Created', detail: `Raspberry Pi "${rpiForm.hostName.trim()}" created`, life: 3000 });
            } catch (e: any) {
                const status = e?.response?.status;
                const data = e?.response?.data;
                let detail: string;
                if (status === 400) {
                    if (data?.message?.includes('already has a RaspberryPi')) {
                        detail = 'This room already has a Raspberry Pi assigned. Choose a different room.';
                    } else if (data && typeof data === 'object' && !data.message) {
                        detail = Object.entries(data).map(([f, m]) => `${f}: ${m}`).join(', ');
                    } else {
                        detail = data?.message ?? 'Invalid input.';
                    }
                } else if (status === 404) {
                    detail = 'Selected room not found. Please refresh and try again.';
                } else if (status === 403) {
                    detail = 'You don\'t have permission to create Raspberry Pis.';
                } else {
                    detail = 'Server error. Please try again later.';
                }
                toast.current?.show({ severity: 'error', summary: 'Creation Failed', detail, life: 5000 });
            }
        } else {
            if (!selectedPi?.id) {
                toast.current?.show({ severity: 'warn', summary: 'Warning', detail: 'No Raspberry Pi selected for editing', life: 3000 });
                return;
            }
            const dto: RaspberryPiUpdateDTO = {};
            if (rpiForm.hostName.trim()) dto.hostName = rpiForm.hostName.trim();
            if (rpiForm.roomId) dto.roomId = rpiForm.roomId;
            try {
                const updated = await RaspberryPiService.update(selectedPi.id, dto);
                setPis(await RaspberryPiService.getAll());
                setRpiDialogVisible(false);
                toast.current?.show({ severity: 'success', summary: 'Updated', detail: `Raspberry Pi "${updated.hostName}" updated`, life: 3000 });
            } catch {
                toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to update Raspberry Pi', life: 3000 });
            }
        }
    };

    const confirmDeleteRpi = (pi: RaspberryPiDTO) => {
        confirmDialog({
            message: `Delete Raspberry Pi "${pi.hostName}"? All associated sensor stations will also be removed.`,
            header: 'Confirm Delete',
            icon: 'pi pi-exclamation-triangle',
            acceptClassName: 'p-button-danger',
            accept: () => void deleteRpi(pi),
        });
    };

    const deleteRpi = async (pi: RaspberryPiDTO) => {
        if (pi.id == null) return;
        try {
            await RaspberryPiService.delete(pi.id);
            setPis(await RaspberryPiService.getAll());
            void fetchDecommissionedPis();
            void fetchAllStations();
            toast.current?.show({ severity: 'success', summary: 'Deleted', detail: `Raspberry Pi "${pi.hostName}" deleted`, life: 3000 });
        } catch {
            toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to delete Raspberry Pi', life: 3000 });
        }
    };



    const openEditStation = (station: SensorStationDTO) => {
        setStationIsNew(false);
        setSelectedStation(station);
        setStationPiId(station.raspberryPiId ?? null);
        setStationForm({
            name: station.name ?? '',
            bleMac: station.bleMac ?? '',
            measurementInterval: station.measurementInterval ?? null,
            roomId: station.roomId ?? null,
            deviceStatus: station.deviceStatus ?? '',
        });
        setStationError(null);
        setStationDialogVisible(true);
    };

    const saveStation = async () => {
        if (!selectedStation?.id) return;
        const isSetup = selectedStation.deviceStatus === SensorStationUpdateDTODeviceStatusEnum.AVAILABLE;
        if (isSetup) {
            if (!stationForm.name.trim()) { setStationError('Name is required'); return; }
            if (stationForm.measurementInterval == null) { setStationError('Measurement Interval is required'); return; }
            if (stationForm.measurementInterval < 3 || stationForm.measurementInterval > 60) { setStationError('Measurement Interval must be between 3 and 60 seconds'); return; }
            try {
                const dto: SensorStationUpdateDTO = {
                    name: stationForm.name.trim(),
                    deviceStatus: SensorStationUpdateDTODeviceStatusEnum.AVAILABLE,
                };
                const updated = await SensorStationService.updateSetup(selectedStation.id, stationForm.measurementInterval, dto);
                if (stationPiId != null) {
                    setStationsMap(prev => ({
                        ...prev,
                        [stationPiId]: (prev[stationPiId] ?? []).some(s => s.id === updated.id)
                            ? (prev[stationPiId] ?? []).map(s => s.id === updated.id ? updated : s)
                            : [...(prev[stationPiId] ?? []), updated],
                    }));
                }
                setStationDialogVisible(false);
                toast.current?.show({ severity: 'success', summary: 'Updated', detail: `Sensor station "${updated.name}" updated`, life: 3000 });
            } catch {
                toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to update sensor station', life: 3000 });
            }
        } else {
            const dto: SensorStationUpdateDTO = {};
            if (stationForm.name.trim()) dto.name = stationForm.name.trim();
            if (stationForm.deviceStatus) dto.deviceStatus = stationForm.deviceStatus as SensorStationUpdateDTODeviceStatusEnum;
            try {
                const updated = await SensorStationService.update(selectedStation.id, dto);
                if (stationPiId != null) {
                    setStationsMap(prev => ({
                        ...prev,
                        [stationPiId]: (prev[stationPiId] ?? []).map(s => s.id === updated.id ? updated : s),
                    }));
                }
                setStationDialogVisible(false);
                toast.current?.show({ severity: 'success', summary: 'Updated', detail: `Sensor station "${updated.name}" updated`, life: 3000 });
            } catch {
                toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to update sensor station', life: 3000 });
            }
        }
    };

    const confirmDeleteStation = (station: SensorStationDTO, piId: number) => {
        confirmDialog({
            message: `Delete sensor station "${station.name}"?`,
            header: 'Confirm Delete',
            icon: 'pi pi-exclamation-triangle',
            acceptClassName: 'p-button-danger',
            accept: () => void deleteStation(station, piId),
        });
    };

    const deleteStation = async (station: SensorStationDTO, piId: number) => {
        if (station.id == null) return;
        try {
            await SensorStationService.delete(station.id);
            setStationsMap(prev => ({
                ...prev,
                [piId]: (prev[piId] ?? []).filter(s => s.id !== station.id),
            }));
            setAllStations(prev =>
                prev.some(s => s.id === station.id)
                    ? prev.map(s => s.id === station.id ? { ...s, deviceStatus: 'DECOMMISSIONED' } : s)
                    : [...prev, { ...station, deviceStatus: 'DECOMMISSIONED' }]
            );
            toast.current?.show({ severity: 'success', summary: 'Deleted', detail: `Sensor station "${station.name}" deleted`, life: 3000 });
        } catch {
            toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to delete sensor station', life: 3000 });
        }
    };

    const roomOptions = rooms.map(r => ({ label: r.name ?? `Room ${r.id}`, value: r.id }));

    const takenRoomIds = new Set(
        pis
            .filter(p => rpiIsNew || p.id !== selectedPi?.id)
            .map(p => p.roomId)
            .filter((id): id is number => id != null)
    );

    const roomItemTemplate = (option: { label: string; value: number }) => {
        const taken = takenRoomIds.has(option.value);
        return (
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span>{option.label}</span>
                {taken && <Tag value="Taken" severity="danger" style={{ fontSize: '0.7rem', marginLeft: '0.5rem' }} />}
            </div>
        );
    };

    const statusTag = (status?: string) => (
        <Tag value={status ?? '—'} severity={statusSeverity(status)} />
    );

    const rowExpansionTemplate = (pi: RaspberryPiDTO) => { // shows sensor stations for each Raspberry Pi
        const piId = pi.id!;
        const stations = stationsMap[piId];

        return (
            <div style={{ padding: '1rem' }}>
                <Card
                    title={`Sensor Stations — ${pi.hostName}`}
                    style={{
                        boxShadow: '0 2px 8px rgba(0,0,0,0.08)',
                        border: '1px solid var(--surface-border)',
                        borderRadius: '6px'
                    }}
                >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1.5rem', flexWrap: 'wrap' }}>
                        <Button
                            label="Scan for Arduinos"
                            icon="pi pi-wifi"
                            size="small"
                            outlined
                            onClick={() => openScanDialog(piId)}
                        />
                    </div>

                    {stations == null ? (
                        <div style={{ display: 'flex', justifyContent: 'center', padding: '1.5rem' }}>
                            <ProgressSpinner style={{ width: '2rem', height: '2rem' }} />
                        </div>
                    ) : (
                        <DataTable
                            value={stations}
                            emptyMessage="No sensor stations registered for this Raspberry Pi."
                            size="small"
                        >
                            <Column field="id" header="ID" style={{ width: '4rem' }} />
                            <Column
                                header="Name"
                                body={(s: SensorStationDTO) => (
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                        <span>{s.name}</span>
                                        {s.name === s.bleMac && (
                                            <Tag
                                                value="Needs Setup"
                                                severity="warning"
                                                icon="pi pi-exclamation-triangle"
                                            />
                                        )}
                                    </div>
                                )}
                            />
                            <Column field="bleMac" header="BLE MAC" />
                            <Column field="measurementInterval" header="Interval (s)" style={{ width: '8rem' }} />
                            <Column
                                header="Status"
                                style={{ width: '9rem' }}
                                body={(s: SensorStationDTO) => statusTag(s.deviceStatus === 'ONLINE' ? undefined : s.deviceStatus)}
                            />
                            <Column
                                header="Actions"
                                style={{ width: '7rem' }}
                                body={(s: SensorStationDTO) => (
                                    <div style={{ display: 'flex', gap: '0.25rem' }}>
                                        <Button
                                            icon="pi pi-pencil"
                                            text
                                            size="small"
                                            onClick={() => openEditStation(s)}
                                            tooltip="Edit"
                                            tooltipOptions={{ position: 'top' }}
                                        />
                                        <Button
                                            icon="pi pi-trash"
                                            text
                                            size="small"
                                            severity="danger"
                                            onClick={() => confirmDeleteStation(s, piId)}
                                            tooltip="Delete"
                                            tooltipOptions={{ position: 'top' }}
                                        />
                                    </div>
                                )}
                            />
                        </DataTable>
                    )}
                </Card>
            </div>
        );
    };

    const rpiFooter = (
        <div>
            <Button label="Cancel" icon="pi pi-times" text onClick={() => setRpiDialogVisible(false)} />
            <Button label={rpiIsNew ? 'Create' : 'Save'} icon="pi pi-check" onClick={() => void saveRpi()} />
        </div>
    );

    const isSetupEdit = !stationIsNew && selectedStation?.deviceStatus === SensorStationUpdateDTODeviceStatusEnum.AVAILABLE;

    const stationFooter = (
        <div>
            <Button label="Cancel" icon="pi pi-times" text onClick={() => setStationDialogVisible(false)} />
            <Button label={stationIsNew ? 'Create' : isSetupEdit ? 'Setup' : 'Save'} icon="pi pi-check" onClick={() => void saveStation()} />
        </div>
    );

    const scanFooter = (
        <div>
            <Button label="Close" icon="pi pi-times" text onClick={() => setScanDialogVisible(false)} />
        </div>
    );

    if (loading) return (
        <div>
            <NavbarComponent />
            <div className="loading-screen">
                <ProgressSpinner />
            </div>
        </div>
    );

    if (error) return (
        <div>
            <NavbarComponent />
            <div style={{ padding: '2rem' }}>
                <Message severity="error" text={error} />
            </div>
        </div>
    );

    return (
        <div>
            <NavbarComponent />
            <Toast ref={toast} />
            <ConfirmDialog />

            <Card title="Device Management" className="m-4">
                <Button
                    label="Add Raspberry Pi"
                    icon="pi pi-plus"
                    raised
                    rounded
                    style={{ marginBottom: '1.5rem' }}
                    onClick={openCreateRpi}
                />

                <section style={{ marginTop: '1rem' }}>
                    <h2 style={{ fontSize: '1.15rem', margin: '0 0 0.75rem' }}>Raspberry Pis</h2>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                        {pis.length === 0 ? (
                            <Message severity="info" text="No Raspberry Pis registered. Click 'Add Raspberry Pi' to get started." />
                        ) : (
                            pis.map((pi) => (
                                <Card
                                    key={pi.id}
                                    style={{
                                        boxShadow: '0 2px 8px rgba(0,0,0,0.08)',
                                        border: '1px solid var(--surface-border)',
                                        borderRadius: '6px'
                                    }}
                                >
                                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr 1fr auto auto', gap: '1.5rem', alignItems: 'center', marginBottom: '0' }}>
                                        <div>
                                            <div style={{ fontSize: '0.75rem', color: 'var(--text-color-secondary)', fontWeight: 600, marginBottom: '0.25rem' }}>ID</div>
                                            <div style={{ fontSize: '1rem', fontWeight: 600 }}>{pi.id}</div>
                                        </div>
                                        <div>
                                            <div style={{ fontSize: '0.75rem', color: 'var(--text-color-secondary)', fontWeight: 600, marginBottom: '0.25rem' }}>Host Name</div>
                                            <div style={{ fontSize: '1rem' }}>{pi.hostName}</div>
                                        </div>
                                        <div>
                                            <div style={{ fontSize: '0.75rem', color: 'var(--text-color-secondary)', fontWeight: 600, marginBottom: '0.25rem' }}>IP Address</div>
                                            <div style={{ fontSize: '1rem' }}>{pi.ipAddress || '—'}</div>
                                        </div>
                                        <div>
                                            <div style={{ fontSize: '0.75rem', color: 'var(--text-color-secondary)', fontWeight: 600, marginBottom: '0.25rem' }}>Status</div>
                                            {statusTag(pi.deviceStatus)}
                                        </div>
                                        <div style={{ display: 'flex', gap: '0.25rem' }}>
                                            <Button
                                                icon="pi pi-pencil"
                                                text
                                                size="small"
                                                onClick={() => openEditRpi(pi)}
                                                tooltip="Edit"
                                                tooltipOptions={{ position: 'top' }}
                                            />
                                            <Button
                                                icon="pi pi-trash"
                                                text
                                                size="small"
                                                severity="danger"
                                                onClick={() => confirmDeleteRpi(pi)}
                                                tooltip="Delete"
                                                tooltipOptions={{ position: 'top' }}
                                            />
                                        </div>
                                        <Button
                                            icon={expandedRows === pi.id ? 'pi pi-chevron-down' : 'pi pi-chevron-right'}
                                            text
                                            rounded
                                            onClick={() => {
                                                if (expandedRows === pi.id) {
                                                    setExpandedRows(null);
                                                } else {
                                                    if (pi.id != null && stationsMap[pi.id] == null) {
                                                        void fetchSensorStations(pi.id);
                                                    }
                                                    setExpandedRows(pi.id);
                                                }
                                            }}
                                            tooltip={expandedRows === pi.id ? 'Collapse' : 'Expand'}
                                            tooltipOptions={{ position: 'top' }}
                                        />
                                    </div>

                                    {!pi.ipAddress && (
                                        <div style={{ marginTop: '0.75rem' }}>
                                            <Message
                                                severity="warn"
                                                text="Setup required: Download conf.yaml, copy it to this Raspberry Pi's SD card, and power it on - the Pi will appear Online once it has booted and connected to the backend"
                                                style={{ width: '100%' }}
                                            />
                                        </div>
                                    )}
                                    <div style={{ marginTop: '0.5rem' }}>
                                        <Button
                                            icon="pi pi-download"
                                            label="Download conf.yaml"
                                            size="small"
                                            severity={pi.ipAddress ? 'secondary' : 'warning'}
                                            outlined
                                            onClick={() => void downloadConfig(pi)}
                                        />
                                    </div>
                                    {expandedRows === pi.id && rowExpansionTemplate(pi)}
                                </Card>
                            ))
                        )}
                    </div>
                </section>

                <section style={{ marginTop: '2rem' }}>
                    <h2 style={{ fontSize: '1.15rem', margin: '0 0 0.75rem' }}>Deleted Raspberry Pis</h2>
                    <DataTable
                        value={decommissionedPis}
                        emptyMessage="No deleted Raspberry Pis."
                        size="small"
                    >
                        <Column field="id" header="ID" style={{ width: '4rem' }} />
                        <Column field="hostName" header="Host Name" />
                        <Column field="ipAddress" header="IP Address" />
                        <Column
                            header="Status"
                            style={{ width: '9rem' }}
                            body={(pi: RaspberryPiDTO) => statusTag(pi.deviceStatus)}
                        />
                    </DataTable>
                </section>

                <section style={{ marginTop: '2rem' }}>
                    <h2 style={{ fontSize: '1.15rem', margin: '0 0 0.75rem' }}>Deleted Sensor Stations</h2>
                    <DataTable
                        value={allStations.filter(s => s.deviceStatus === 'DECOMMISSIONED')}
                        emptyMessage="No deleted sensor stations."
                        size="small"
                    >
                        <Column field="id" header="ID" style={{ width: '4rem' }} />
                        <Column field="name" header="Name" />
                        <Column field="bleMac" header="BLE MAC" />
                        <Column field="measurementInterval" header="Interval (s)" style={{ width: '8rem' }} />
                        <Column
                            header="Status"
                            style={{ width: '9rem' }}
                            body={(s: SensorStationDTO) => statusTag(s.deviceStatus)}
                        />
                    </DataTable>
                </section>
            </Card>

            {/* Raspberry Pi Create/Edit Dialog */}
            <Dialog
                header={rpiIsNew ? 'Add Raspberry Pi' : `Edit Raspberry Pi — ${selectedPi?.hostName ?? ''}`}
                visible={rpiDialogVisible}
                style={{ width: '420px' }}
                onHide={() => setRpiDialogVisible(false)}
                footer={rpiFooter}
            >
                {rpiError && (
                    <Message severity="error" text={rpiError} style={{ marginBottom: '1rem', width: '100%' }} />
                )}
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                    <div>
                        <label style={{ display: 'block', marginBottom: '0.25rem', fontWeight: 600 }}>
                            Host Name {rpiIsNew && <span style={{ color: 'var(--red-500)' }}>*</span>}
                        </label>
                        <InputText
                            value={rpiForm.hostName}
                            onChange={e => setRpiForm(f => ({ ...f, hostName: e.target.value }))}
                            style={{ width: '100%' }}
                            placeholder="e.g. pi-office-floor2"
                        />
                    </div>
                    <div>
                        <label style={{ display: 'block', marginBottom: '0.25rem', fontWeight: 600 }}>
                            Room {rpiIsNew && <span style={{ color: 'var(--red-500)' }}>*</span>}
                        </label>
                        <Dropdown
                            value={rpiForm.roomId}
                            options={roomOptions}
                            itemTemplate={roomItemTemplate}
                            onChange={e => {
                                if (takenRoomIds.has(e.value)) {
                                    toast.current?.show({ severity: 'warn', summary: 'Room already assigned', detail: 'This room already has a Raspberry Pi. Choose a different room.', life: 4000 });
                                    return;
                                }
                                setRpiForm(f => ({ ...f, roomId: e.value }));
                            }}
                            placeholder="Select a room"
                            style={{ width: '100%' }}
                        />
                    </div>
                </div>
            </Dialog>

            {/*SensorStation Create/Edit Dialog */}
            <Dialog
                header={stationIsNew ? 'Add Sensor Station' : isSetupEdit ? `Setup Sensor Station — ${selectedStation?.bleMac ?? ''}` : `Edit Sensor Station — ${selectedStation?.name ?? ''}`}
                visible={stationDialogVisible}
                style={{ width: '440px' }}
                onHide={() => setStationDialogVisible(false)}
                footer={stationFooter}
            >
                {stationError && (
                    <Message severity="error" text={stationError} style={{ marginBottom: '1rem', width: '100%' }} />
                )}
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                    <div>
                        <label style={{ display: 'block', marginBottom: '0.25rem', fontWeight: 600 }}>
                            Name <span style={{ color: 'var(--red-500)' }}>*</span>
                        </label>
                        <InputText
                            value={stationForm.name}
                            onChange={e => setStationForm(f => ({ ...f, name: e.target.value }))}
                            style={{ width: '100%' }}
                            placeholder="e.g. Office Sensor A"
                        />
                    </div>

                    {stationIsNew && (
                        <>
                            <div>
                                <label style={{ display: 'block', marginBottom: '0.25rem', fontWeight: 600 }}>
                                    BLE MAC Address <span style={{ color: 'var(--red-500)' }}>*</span>
                                </label>
                                <InputText
                                    value={stationForm.bleMac}
                                    onChange={e => setStationForm(f => ({ ...f, bleMac: e.target.value }))}
                                    style={{ width: '100%' }}
                                    placeholder="e.g. AA:BB:CC:DD:EE:FF"
                                />
                            </div>
                            <div>
                                <label style={{ display: 'block', marginBottom: '0.25rem', fontWeight: 600 }}>
                                    Measurement Interval (s) <span style={{ color: 'var(--red-500)' }}>*</span>
                                </label>
                                <InputNumber
                                    value={stationForm.measurementInterval}
                                    onValueChange={e => setStationForm(f => ({ ...f, measurementInterval: e.value ?? null }))}
                                    style={{ width: '100%' }}
                                    inputStyle={{ width: '100%' }}
                                    min={3}
                                    max={60}
                                    placeholder="3 – 60 s"
                                />
                            </div>
                            <div>
                                <label style={{ display: 'block', marginBottom: '0.25rem', fontWeight: 600 }}>
                                    Room <span style={{ color: 'var(--red-500)' }}>*</span>
                                </label>
                                <Dropdown
                                    value={stationForm.roomId}
                                    options={roomOptions}
                                    onChange={e => setStationForm(f => ({ ...f, roomId: e.value }))}
                                    placeholder="Select a room"
                                    style={{ width: '100%' }}
                                />
                            </div>
                        </>
                    )}

                    {!stationIsNew && selectedStation?.deviceStatus === SensorStationUpdateDTODeviceStatusEnum.AVAILABLE && (
                        <div>
                            <label style={{ display: 'block', marginBottom: '0.25rem', fontWeight: 600 }}>
                                Measurement Interval (s) <span style={{ color: 'var(--red-500)' }}>*</span>
                            </label>
                            <InputNumber
                                value={stationForm.measurementInterval}
                                onValueChange={e => setStationForm(f => ({ ...f, measurementInterval: e.value ?? null }))}
                                style={{ width: '100%' }}
                                inputStyle={{ width: '100%' }}
                                min={3}
                                max={60}
                                placeholder="3 – 60 s"
                            />
                        </div>
                    )}


                </div>
            </Dialog>

            {/* BLE Scan Dialog */}
            <Dialog
                header="Scan for Arduino Devices"
                visible={scanDialogVisible}
                style={{ width: '520px' }}
                onHide={() => setScanDialogVisible(false)}
                footer={scanFooter}
            >
                <p style={{ color: 'var(--text-color-secondary)', fontSize: '0.875rem', marginBottom: '1rem' }}>
                    The Raspberry Pi will scan for nearby Arduino devices in setup mode (takes ~10 seconds).
                    Select a found device to register it as a sensor station.
                </p>

                <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1rem', flexWrap: 'wrap' }}>
                    <Button
                        label={scanning ? 'Scanning…' : 'Start Scan'}
                        icon={scanning ? 'pi pi-spin pi-spinner' : 'pi pi-wifi'}
                        disabled={scanning}
                        onClick={() => void startScan()}
                    />
                </div>

                {scanTriggered && availableStations.length === 0 && (
                    <Message
                        severity="info"
                        text={
                            scanCountdown != null && scanCountdown > 0
                                ? `Scanning… ${scanCountdown}s remaining`
                                : 'Scan complete — watching for nearby Arduino devices every 5 seconds. You\'ll be notified when one is found.'
                        }
                        style={{ width: '100%', marginBottom: '1rem' }}
                    />
                )}

                {!scanTriggered && !scanning && (
                    <p style={{ color: 'var(--text-color-secondary)', textAlign: 'center', padding: '1rem 0' }}>
                        No devices found yet. Press "Start Scan" to discover nearby Arduino devices.
                    </p>
                )}

                {availableStations.length > 0 && (
                    <DataTable value={availableStations} size="small">
                        <Column field="bleMac" header="BLE MAC Address" />
                        <Column
                            header="Action"
                            style={{ width: '8rem' }}
                            body={(s: SensorStationDTO) => (
                                <Button
                                    label="Select"
                                    icon="pi pi-check"
                                    size="small"
                                    severity="success"
                                    onClick={() => selectAvailableStation(s)}
                                />
                            )}
                        />
                    </DataTable>
                )}
            </Dialog>
        </div>
    );
};

export default DeviceManagementView;

import React, { useEffect, useRef, useState } from 'react';
import { Button } from 'primereact/button';
import { Column } from 'primereact/column';
import { ConfirmDialog, confirmDialog } from 'primereact/confirmdialog';
import { DataTable } from 'primereact/datatable';
import { Dialog } from 'primereact/dialog';
import { Dropdown } from 'primereact/dropdown';
import { InputText } from 'primereact/inputtext';
import { Message } from 'primereact/message';
import { ProgressSpinner } from 'primereact/progressspinner';
import { Toast } from 'primereact/toast';
import 'primeicons/primeicons.css';

import NavbarComponent from '../components/NavbarComponent';
import { FooterComponent } from '../components/FooterComponent';
import { AddressService } from '../services/AddressService';
import { BuildingService } from '../services/BuildingService';
import { DepartmentService } from '../services/DepartmentService';
import { apiConfig } from '../services/apiConfig';
import {
    AddressDTO,
    AdminControllerApi,
    BuildingDTO,
    DepartmentDTO,
    UserxDTO,
} from '../generated-skeleton-api';

type Tab = 'buildings' | 'addresses' | 'departments';

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

// ── Address form ──────────────────────────────────────────────────────────────

interface AddressForm {
    country: string;
    zipCode: string;
    city: string;
    street: string;
    houseNumber: string;
    extra: string;
}
const EMPTY_ADDRESS: AddressForm = { country: '', zipCode: '', city: '', street: '', houseNumber: '', extra: '' };

// ── Building form ─────────────────────────────────────────────────────────────

interface BuildingForm { name: string; addressId: number | null }
const EMPTY_BUILDING: BuildingForm = { name: '', addressId: null };

// ── Department form ───────────────────────────────────────────────────────────

interface DeptForm { name: string; departmentLeadId: number | null }
const EMPTY_DEPT: DeptForm = { name: '', departmentLeadId: null };

// ─────────────────────────────────────────────────────────────────────────────

const OrgStructureView: React.FC = () => {
    const toast = useRef<Toast>(null);

    const [activeTab, setActiveTab] = useState<Tab>('buildings');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const [addresses, setAddresses] = useState<AddressDTO[]>([]);
    const [buildings, setBuildings] = useState<BuildingDTO[]>([]);
    const [departments, setDepartments] = useState<DepartmentDTO[]>([]);
    const [users, setUsers] = useState<UserxDTO[]>([]);

    // ── Dialog state ──────────────────────────────────────────────────────────
    const [addrDialog, setAddrDialog] = useState(false);
    const [addrForm, setAddrForm] = useState<AddressForm>(EMPTY_ADDRESS);
    const [editingAddr, setEditingAddr] = useState<AddressDTO | null>(null);

    const [bldgDialog, setBldgDialog] = useState(false);
    const [bldgForm, setBldgForm] = useState<BuildingForm>(EMPTY_BUILDING);
    const [editingBldg, setEditingBldg] = useState<BuildingDTO | null>(null);

    const [deptDialog, setDeptDialog] = useState(false);
    const [deptForm, setDeptForm] = useState<DeptForm>(EMPTY_DEPT);
    const [editingDept, setEditingDept] = useState<DepartmentDTO | null>(null);

    const [saving, setSaving] = useState(false);

    // ── Initial load ──────────────────────────────────────────────────────────

    useEffect(() => {
        const load = async () => {
            try {
                const [addrData, bldgData, deptData] = await Promise.all([
                    AddressService.getAll(),
                    BuildingService.getAll(),
                    DepartmentService.getAll(),
                ]);
                setAddresses(addrData);
                setBuildings(bldgData);
                setDepartments(deptData);

                try {
                    const adminApi = new AdminControllerApi(apiConfig);
                    const userData = await adminApi.getAllUsers().then(r => r.data);
                    setUsers(userData);
                } catch {
                    // insufficient permissions — lead dropdown stays empty
                }
            } catch (err) {
                setError(err instanceof Error ? err.message : String(err));
            } finally {
                setLoading(false);
            }
        };
        void load();
    }, []);

    // ── Helpers ───────────────────────────────────────────────────────────────

    const showSuccess = (msg: string) =>
        toast.current?.show({ severity: 'success', summary: 'Success', detail: msg, life: 3000 });
    const showError = (msg: string) =>
        toast.current?.show({ severity: 'error', summary: 'Error', detail: msg, life: 4000 });

    const addressLabel = (id?: number) => {
        if (!id) return '—';
        const a = addresses.find(x => x.id === id);
        if (!a) return `Address ${id}`;
        return `${a.street ?? ''} ${a.houseNumber ?? ''}, ${a.zipCode ?? ''} ${a.city ?? ''}`.trim();
    };

    const userName = (id?: number) => {
        if (!id) return '—';
        const u = users.find(x => x.id === id);
        if (!u) return `User ${id}`;
        return (`${u.firstName ?? ''} ${u.lastName ?? ''}`).trim() || u.username || `User ${id}`;
    };

    // ── Address CRUD ──────────────────────────────────────────────────────────

    const openCreateAddr = () => { setEditingAddr(null); setAddrForm(EMPTY_ADDRESS); setAddrDialog(true); };
    const openEditAddr = (a: AddressDTO) => {
        setEditingAddr(a);
        setAddrForm({ country: a.country ?? '', zipCode: a.zipCode ?? '', city: a.city ?? '', street: a.street ?? '', houseNumber: a.houseNumber ?? '', extra: a.extra ?? '' });
        setAddrDialog(true);
    };
    const saveAddr = async () => {
        if (!addrForm.country || !addrForm.zipCode || !addrForm.city || !addrForm.street || !addrForm.houseNumber) {
            showError('Please fill in all required address fields.');
            return;
        }
        setSaving(true);
        try {
            if (editingAddr?.id) {
                const updated = await AddressService.update(editingAddr.id, addrForm);
                setAddresses(prev => prev.map(a => a.id === updated.id ? updated : a));
                showSuccess('Address updated.');
            } else {
                const created = await AddressService.create(addrForm);
                setAddresses(prev => [...prev, created]);
                showSuccess('Address created.');
            }
            setAddrDialog(false);
        } catch (err) {
            showError(err instanceof Error ? err.message : 'Failed to save address.');
        } finally {
            setSaving(false);
        }
    };
    const deleteAddr = (a: AddressDTO) => {
        confirmDialog({
            message: `Delete address "${a.street} ${a.houseNumber}, ${a.city}"?`,
            header: 'Confirm Delete',
            icon: 'pi pi-trash',
            acceptClassName: 'p-button-danger',
            accept: async () => {
                try {
                    await AddressService.delete(a.id!);
                    setAddresses(prev => prev.filter(x => x.id !== a.id));
                    showSuccess('Address deleted.');
                } catch (err) {
                    showError(err instanceof Error ? err.message : 'Failed to delete address.');
                }
            },
        });
    };

    // ── Building CRUD ─────────────────────────────────────────────────────────

    const openCreateBldg = () => { setEditingBldg(null); setBldgForm(EMPTY_BUILDING); setBldgDialog(true); };
    const openEditBldg = (b: BuildingDTO) => {
        setEditingBldg(b);
        setBldgForm({ name: b.name ?? '', addressId: b.addressId ?? null });
        setBldgDialog(true);
    };
    const saveBldg = async () => {
        if (!bldgForm.name.trim() || !bldgForm.addressId) {
            showError('Please provide a name and select an address.');
            return;
        }
        setSaving(true);
        try {
            if (editingBldg?.id) {
                const updated = await BuildingService.update(editingBldg.id, { name: bldgForm.name, addressId: bldgForm.addressId });
                setBuildings(prev => prev.map(b => b.id === updated.id ? updated : b));
                showSuccess('Building updated.');
            } else {
                const created = await BuildingService.create({ name: bldgForm.name, addressId: bldgForm.addressId });
                setBuildings(prev => [...prev, created]);
                showSuccess('Building created.');
            }
            setBldgDialog(false);
        } catch (err) {
            showError(err instanceof Error ? err.message : 'Failed to save building.');
        } finally {
            setSaving(false);
        }
    };
    const deleteBldg = (b: BuildingDTO) => {
        confirmDialog({
            message: `Delete building "${b.name}"?`,
            header: 'Confirm Delete',
            icon: 'pi pi-trash',
            acceptClassName: 'p-button-danger',
            accept: async () => {
                try {
                    await BuildingService.delete(b.id!);
                    setBuildings(prev => prev.filter(x => x.id !== b.id));
                    showSuccess('Building deleted.');
                } catch (err) {
                    showError(err instanceof Error ? err.message : 'Failed to delete building.');
                }
            },
        });
    };

    // ── Department CRUD ───────────────────────────────────────────────────────

    const openCreateDept = () => { setEditingDept(null); setDeptForm(EMPTY_DEPT); setDeptDialog(true); };
    const openEditDept = (d: DepartmentDTO) => {
        setEditingDept(d);
        setDeptForm({ name: d.name ?? '', departmentLeadId: d.departmentLeadId ?? null });
        setDeptDialog(true);
    };
    const saveDept = async () => {
        if (!deptForm.name.trim() || !deptForm.departmentLeadId) {
            showError('Please provide a name and select a department lead.');
            return;
        }
        setSaving(true);
        try {
            if (editingDept?.id) {
                const updated = await DepartmentService.update(editingDept.id, { name: deptForm.name, departmentLeadId: deptForm.departmentLeadId });
                setDepartments(prev => prev.map(d => d.id === updated.id ? updated : d));
                showSuccess('Department updated.');
            } else {
                const created = await DepartmentService.create({ name: deptForm.name, departmentLeadId: deptForm.departmentLeadId });
                setDepartments(prev => [...prev, created]);
                showSuccess('Department created.');
            }
            setDeptDialog(false);
        } catch (err) {
            showError(err instanceof Error ? err.message : 'Failed to save department.');
        } finally {
            setSaving(false);
        }
    };
    const deleteDept = (d: DepartmentDTO) => {
        confirmDialog({
            message: `Delete department "${d.name}"?`,
            header: 'Confirm Delete',
            icon: 'pi pi-trash',
            acceptClassName: 'p-button-danger',
            accept: async () => {
                try {
                    await DepartmentService.delete(d.id!);
                    setDepartments(prev => prev.filter(x => x.id !== d.id));
                    showSuccess('Department deleted.');
                } catch (err) {
                    showError(err instanceof Error ? err.message : 'Failed to delete department.');
                }
            },
        });
    };

    // ── Render ────────────────────────────────────────────────────────────────

    if (loading) {
        return (
            <div>
                <NavbarComponent />
                <div className="flex justify-content-center align-items-center" style={{ minHeight: '60vh' }}>
                    <ProgressSpinner />
                </div>
            </div>
        );
    }

    if (error) {
        return (
            <div>
                <NavbarComponent />
                <div className="m-4"><Message severity="error" text={error} /></div>
            </div>
        );
    }

    const actionTemplate = (onEdit: () => void, onDelete: () => void) => (
        <div style={{ display: 'flex', gap: '0.5rem' }}>
            <Button icon="pi pi-pencil" size="small" severity="secondary" outlined onClick={onEdit} />
            <Button icon="pi pi-trash" size="small" severity="danger" outlined onClick={onDelete} />
        </div>
    );

    const addressOptions = addresses.map(a => ({
        label: addressLabel(a.id),
        value: a.id,
    }));

    const userOptions = users.map(u => ({
        label: (`${u.firstName ?? ''} ${u.lastName ?? ''}`).trim() || u.username || `User ${u.id}`,
        value: u.id,
    }));

    return (
        <div>
            <NavbarComponent />
            <Toast ref={toast} />
            <ConfirmDialog />

            <div style={{ padding: '1.5rem 2rem', maxWidth: '1400px', margin: '0 auto' }}>
                {/* Header */}
                <div style={{ marginBottom: '2rem', padding: '1.5rem 2rem', backgroundColor: '#f8f9fa', borderRadius: '12px', border: '1px solid #e9ecef', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
                    <h1 style={{ margin: 0, color: '#111827', fontSize: '2rem', fontWeight: 700 }}>Organization</h1>
                    <p style={{ margin: '0.5rem 0 0', color: '#6b7280', fontSize: '0.95rem' }}>Manage buildings, addresses, and departments</p>
                </div>

                {/* Tabs */}
                <div style={{ display: 'flex', borderBottom: '2px solid #e5e7eb', marginBottom: '2rem', backgroundColor: '#ffffff', borderRadius: '12px 12px 0 0', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
                    <button style={TAB_STYLE(activeTab === 'buildings')} onClick={() => setActiveTab('buildings')}>
                        <i className="pi pi-building" style={{ marginRight: '0.5rem' }} />
                        Buildings
                    </button>
                    <button style={TAB_STYLE(activeTab === 'addresses')} onClick={() => setActiveTab('addresses')}>
                        <i className="pi pi-map-marker" style={{ marginRight: '0.5rem' }} />
                        Addresses
                    </button>
                    <button style={TAB_STYLE(activeTab === 'departments')} onClick={() => setActiveTab('departments')}>
                        <i className="pi pi-sitemap" style={{ marginRight: '0.5rem' }} />
                        Departments
                    </button>
                </div>

                <div style={{ padding: '2rem', backgroundColor: '#ffffff', borderRadius: '0 12px 12px 12px', border: '1px solid #e5e7eb', borderTop: 'none' }}>

                    {/* ── Buildings Tab ── */}
                    {activeTab === 'buildings' && (
                        <>
                            <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '1rem' }}>
                                <Button label="Add Building" icon="pi pi-plus" onClick={openCreateBldg} />
                            </div>
                            <DataTable value={buildings} stripedRows paginator rows={10} emptyMessage="No buildings found.">
                                <Column field="id" header="ID" style={{ width: '5rem' }} />
                                <Column field="name" header="Name" />
                                <Column header="Address" body={(b: BuildingDTO) => addressLabel(b.addressId)} />
                                <Column header="Actions" style={{ width: '8rem' }} body={(b: BuildingDTO) => actionTemplate(() => openEditBldg(b), () => deleteBldg(b))} />
                            </DataTable>
                        </>
                    )}

                    {/* ── Addresses Tab ── */}
                    {activeTab === 'addresses' && (
                        <>
                            <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '1rem' }}>
                                <Button label="Add Address" icon="pi pi-plus" onClick={openCreateAddr} />
                            </div>
                            <DataTable value={addresses} stripedRows paginator rows={10} emptyMessage="No addresses found.">
                                <Column field="id" header="ID" style={{ width: '5rem' }} />
                                <Column field="country" header="Country" />
                                <Column field="zipCode" header="ZIP" />
                                <Column field="city" header="City" />
                                <Column field="street" header="Street" />
                                <Column field="houseNumber" header="No." style={{ width: '5rem' }} />
                                <Column field="extra" header="Extra" />
                                <Column header="Actions" style={{ width: '8rem' }} body={(a: AddressDTO) => actionTemplate(() => openEditAddr(a), () => deleteAddr(a))} />
                            </DataTable>
                        </>
                    )}

                    {/* ── Departments Tab ── */}
                    {activeTab === 'departments' && (
                        <>
                            <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '1rem' }}>
                                <Button label="Add Department" icon="pi pi-plus" onClick={openCreateDept} />
                            </div>
                            <DataTable value={departments} stripedRows paginator rows={10} emptyMessage="No departments found.">
                                <Column field="id" header="ID" style={{ width: '5rem' }} />
                                <Column field="name" header="Name" />
                                <Column header="Department Lead" body={(d: DepartmentDTO) => userName(d.departmentLeadId)} />
                                <Column header="Actions" style={{ width: '8rem' }} body={(d: DepartmentDTO) => actionTemplate(() => openEditDept(d), () => deleteDept(d))} />
                            </DataTable>
                        </>
                    )}
                </div>
            </div>

            {/* ── Address Dialog ── */}
            <Dialog
                header={editingAddr ? 'Edit Address' : 'New Address'}
                visible={addrDialog}
                style={{ width: '480px' }}
                onHide={() => setAddrDialog(false)}
                footer={
                    <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'flex-end' }}>
                        <Button label="Cancel" severity="secondary" outlined onClick={() => setAddrDialog(false)} />
                        <Button label="Save" icon="pi pi-check" loading={saving} onClick={() => void saveAddr()} />
                    </div>
                }
            >
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', paddingTop: '0.5rem' }}>
                    {(['country', 'zipCode', 'city', 'street', 'houseNumber'] as const).map(field => (
                        <div key={field}>
                            <label style={{ display: 'block', fontWeight: 600, fontSize: '0.875rem', marginBottom: '0.4rem', color: '#374151' }}>
                                {field === 'zipCode' ? 'ZIP Code' : field === 'houseNumber' ? 'House Number' : field.charAt(0).toUpperCase() + field.slice(1)}
                                {' *'}
                            </label>
                            <InputText
                                value={addrForm[field]}
                                onChange={e => setAddrForm(f => ({ ...f, [field]: e.target.value }))}
                                style={{ width: '100%' }}
                            />
                        </div>
                    ))}
                    <div>
                        <label style={{ display: 'block', fontWeight: 600, fontSize: '0.875rem', marginBottom: '0.4rem', color: '#374151' }}>Extra</label>
                        <InputText
                            value={addrForm.extra}
                            onChange={e => setAddrForm(f => ({ ...f, extra: e.target.value }))}
                            style={{ width: '100%' }}
                            placeholder="Floor, building wing, etc."
                        />
                    </div>
                </div>
            </Dialog>

            {/* ── Building Dialog ── */}
            <Dialog
                header={editingBldg ? 'Edit Building' : 'New Building'}
                visible={bldgDialog}
                style={{ width: '420px' }}
                onHide={() => setBldgDialog(false)}
                footer={
                    <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'flex-end' }}>
                        <Button label="Cancel" severity="secondary" outlined onClick={() => setBldgDialog(false)} />
                        <Button label="Save" icon="pi pi-check" loading={saving} onClick={() => void saveBldg()} />
                    </div>
                }
            >
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', paddingTop: '0.5rem' }}>
                    <div>
                        <label style={{ display: 'block', fontWeight: 600, fontSize: '0.875rem', marginBottom: '0.4rem', color: '#374151' }}>Name *</label>
                        <InputText value={bldgForm.name} onChange={e => setBldgForm(f => ({ ...f, name: e.target.value }))} style={{ width: '100%' }} />
                    </div>
                    <div>
                        <label style={{ display: 'block', fontWeight: 600, fontSize: '0.875rem', marginBottom: '0.4rem', color: '#374151' }}>Address *</label>
                        <Dropdown
                            value={bldgForm.addressId}
                            options={addressOptions}
                            onChange={e => setBldgForm(f => ({ ...f, addressId: e.value as number }))}
                            placeholder="Select address…"
                            filter
                            style={{ width: '100%' }}
                        />
                    </div>
                </div>
            </Dialog>

            {/* ── Department Dialog ── */}
            <Dialog
                header={editingDept ? 'Edit Department' : 'New Department'}
                visible={deptDialog}
                style={{ width: '420px' }}
                onHide={() => setDeptDialog(false)}
                footer={
                    <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'flex-end' }}>
                        <Button label="Cancel" severity="secondary" outlined onClick={() => setDeptDialog(false)} />
                        <Button label="Save" icon="pi pi-check" loading={saving} onClick={() => void saveDept()} />
                    </div>
                }
            >
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', paddingTop: '0.5rem' }}>
                    <div>
                        <label style={{ display: 'block', fontWeight: 600, fontSize: '0.875rem', marginBottom: '0.4rem', color: '#374151' }}>Name *</label>
                        <InputText value={deptForm.name} onChange={e => setDeptForm(f => ({ ...f, name: e.target.value }))} style={{ width: '100%' }} />
                    </div>
                    <div>
                        <label style={{ display: 'block', fontWeight: 600, fontSize: '0.875rem', marginBottom: '0.4rem', color: '#374151' }}>Department Lead *</label>
                        <Dropdown
                            value={deptForm.departmentLeadId}
                            options={userOptions}
                            onChange={e => setDeptForm(f => ({ ...f, departmentLeadId: e.value as number }))}
                            placeholder="Select user…"
                            filter
                            style={{ width: '100%' }}
                        />
                    </div>
                </div>
            </Dialog>

            <FooterComponent />
        </div>
    );
};

export default OrgStructureView;

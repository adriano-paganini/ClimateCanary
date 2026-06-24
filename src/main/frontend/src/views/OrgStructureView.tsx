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
import { Tag } from 'primereact/tag';
import { Toast } from 'primereact/toast';
import 'primeicons/primeicons.css';

import NavbarComponent from '../components/NavbarComponent';
import UserDialog from '../components/UserDialog';
import { AddressService } from '../services/AddressService';
import { BuildingService } from '../services/BuildingService';
import { DepartmentService } from '../services/DepartmentService';
import { EmployeeProfileService } from '../services/EmployeeProfileService';
import { RoomService } from '../services/RoomService';
import { UserService } from '../services/UserService';
import { apiConfig } from '../services/apiConfig';
import {
    AddressDTO,
    AdminControllerApi,
    BuildingDTO,
    DepartmentDTO,
    EmployeeProfileDTO,
    RoomCreateDTO,
    RoomDTO,
    RoomType,
    RoomUpdateDTO,
    UserxCreateDTO,
    UserxDTO,
    UserxRole,
    UserxUpdateDTO,
} from '../generated-skeleton-api';
import { createUserxRoleArrayFromStrings, rolesToArray, UserxValidationResult } from '../utilities/userxUtilities';

type Tab = 'buildings' | 'addresses' | 'departments' | 'rooms';

function apiError(err: unknown, fallback: string, conflictMsg?: string): string {
    const response = (err as { response?: { status?: number; data?: { message?: string } } })?.response;
    if (response) {
        const { status, data } = response;
        if (status === 409) return conflictMsg ?? 'This item is still referenced by other data and cannot be changed.';
        if (status === 404) return 'Item not found. Please refresh the page.';
        if (status === 403) return 'You do not have permission for this action.';
        if (status === 400) return data?.message ?? 'Invalid input. Please check your data.';
        if (data?.message) return data.message;
    }
    return err instanceof Error ? err.message : fallback;
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

interface AddressForm {
    country: string;
    zipCode: string;
    city: string;
    street: string;
    houseNumber: string;
    extra: string;
}
const EMPTY_ADDRESS: AddressForm = { country: '', zipCode: '', city: '', street: '', houseNumber: '', extra: '' };

interface BuildingForm { name: string; addressId: number | null }
const EMPTY_BUILDING: BuildingForm = { name: '', addressId: null };

interface DeptForm { name: string; departmentLeadId: number | null }
const EMPTY_DEPT: DeptForm = { name: '', departmentLeadId: null };

interface RoomForm {
    name: string;
    roomType?: RoomType;
    privacyMode: boolean;
    departmentId?: number;
    buildingId?: number;
}
const EMPTY_ROOM: RoomForm = { name: '', roomType: undefined, privacyMode: false, departmentId: undefined, buildingId: undefined };

const EMPTY_EMPLOYEE_USER: UserxCreateDTO = {
    username: '',
    password: '',
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    enabled: true,
    roles: new Set([UserxRole.EMPLOYEE]),
};

const OrgStructureView: React.FC = () => {
    const toast = useRef<Toast>(null);

    const [activeTab, setActiveTab] = useState<Tab>('buildings');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const [addresses, setAddresses] = useState<AddressDTO[]>([]);
    const [buildings, setBuildings] = useState<BuildingDTO[]>([]);
    const [departments, setDepartments] = useState<DepartmentDTO[]>([]);
    const [rooms, setRooms] = useState<RoomDTO[]>([]);
    const [users, setUsers] = useState<UserxDTO[]>([]);
    const [employeeProfiles, setEmployeeProfiles] = useState<EmployeeProfileDTO[]>([]);

    const [addressFilter, setAddressFilter] = useState('');
    const [buildingFilter, setBuildingFilter] = useState('');
    const [departmentFilter, setDepartmentFilter] = useState('');
    const [roomFilter, setRoomFilter] = useState('');
    const [expandedDepartments, setExpandedDepartments] = useState<Record<string, boolean>>({});

    const [addrDialog, setAddrDialog] = useState(false);
    const [addrForm, setAddrForm] = useState<AddressForm>(EMPTY_ADDRESS);
    const [editingAddr, setEditingAddr] = useState<AddressDTO | null>(null);

    const [bldgDialog, setBldgDialog] = useState(false);
    const [bldgForm, setBldgForm] = useState<BuildingForm>(EMPTY_BUILDING);
    const [editingBldg, setEditingBldg] = useState<BuildingDTO | null>(null);

    const [deptDialog, setDeptDialog] = useState(false);
    const [deptForm, setDeptForm] = useState<DeptForm>(EMPTY_DEPT);
    const [editingDept, setEditingDept] = useState<DepartmentDTO | null>(null);

    const [roomDialog, setRoomDialog] = useState(false);
    const [roomForm, setRoomForm] = useState<RoomForm>(EMPTY_ROOM);
    const [editingRoom, setEditingRoom] = useState<RoomDTO | null>(null);

    const [assignDialog, setAssignDialog] = useState(false);
    const [createEmployeeDialog, setCreateEmployeeDialog] = useState(false);
    const [targetDepartment, setTargetDepartment] = useState<DepartmentDTO | null>(null);
    const [targetRooms, setTargetRooms] = useState<RoomDTO[]>([]);
    const [assignUserId, setAssignUserId] = useState<number | null>(null);
    const [assignRoomId, setAssignRoomId] = useState<number | null>(null);
    const [createEmployeeForm, setCreateEmployeeForm] = useState<UserxCreateDTO>(EMPTY_EMPLOYEE_USER);
    const [createEmployeeDepartmentId, setCreateEmployeeDepartmentId] = useState<number | undefined>(undefined);
    const [createEmployeeRoomId, setCreateEmployeeRoomId] = useState<number | undefined>(undefined);
    const [createEmployeeValidation, setCreateEmployeeValidation] = useState<UserxValidationResult>({ valid: true });

    const [saving, setSaving] = useState(false);


    useEffect(() => {
        const load = async () => {
            try {
                const [addrData, bldgData, deptData, roomData, profileData] = await Promise.all([
                    AddressService.getAll(),
                    BuildingService.getAll(),
                    DepartmentService.getAll(),
                    RoomService.getAll(),
                    EmployeeProfileService.getAll(),
                ]);
                setAddresses(addrData);
                setBuildings(bldgData);
                setDepartments(deptData);
                setRooms(roomData);
                setEmployeeProfiles(profileData);

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

    const roomName = (id?: number) => {
        if (!id) return '—';
        return rooms.find(r => r.id === id)?.name ?? `Room ${id}`;
    };

    const departmentName = (id?: number) => {
        if (!id) return '—';
        return departments.find(d => d.id === id)?.name ?? `Department ${id}`;
    };

    const buildingName = (id?: number) => {
        if (!id) return '—';
        return buildings.find(b => b.id === id)?.name ?? `Building ${id}`;
    };

    const isOnlyDepartmentLeadRole = (user?: UserxDTO): boolean => {
        const roles = rolesToArray(user?.roles);
        return roles.length === 1 && roles.includes(UserxRole.DEPARTMENT_LEAD);
    };

    const getDepartmentsLedByUser = (userId?: number): DepartmentDTO[] => {
        if (!userId) return [];
        return departments.filter(department => department.departmentLeadId === userId);
    };

    const wouldLeaveLeadWithoutRole = (department: DepartmentDTO): boolean => {
        if (!department.departmentLeadId) return false;

        const lead = users.find(user => user.id === department.departmentLeadId);
        if (!lead) return false;

        return getDepartmentsLedByUser(lead.id).length === 1 && isOnlyDepartmentLeadRole(lead);
    };

    const getDepartmentLeadBlockReason = (department: DepartmentDTO): string | null => {
        if (!wouldLeaveLeadWithoutRole(department)) return null;

        const leadName = userName(department.departmentLeadId);

        return `Assign ${leadName} as lead of another department.`;
    };

    const pluralizeEmployee = (count: number): string =>
        `${count} employee${count === 1 ? '' : 's'}`;

    const countDepartmentEmployees = (departmentId?: number): number => {
        if (!departmentId) return 0;
        return employeeProfiles.filter(profile => profile.departmentId === departmentId).length;
    };

    const countRoomEmployees = (roomId?: number): number => {
        if (!roomId) return 0;
        return employeeProfiles.filter(profile => profile.roomId === roomId).length;
    };

    const leadCanBeChanged = (department: DepartmentDTO | null): boolean => {
        if (!department) return true;
        return !wouldLeaveLeadWithoutRole(department);
    };


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
            showError(apiError(err, 'Failed to save address.', 'An address with these details already exists.'));
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
                    showError(apiError(err, 'Failed to delete address.', 'This address is still used by one or more buildings and cannot be deleted.'));
                }
            },
        });
    };


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
            showError(apiError(err, 'Failed to save building.', 'A building with this name already exists.'));
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
                    showError(apiError(err, 'Failed to delete building.', 'This building still has rooms assigned and cannot be deleted.'));
                }
            },
        });
    };


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

        if (
            editingDept?.departmentLeadId &&
            editingDept.departmentLeadId !== deptForm.departmentLeadId &&
            !leadCanBeChanged(editingDept)
        ) {
            showError(getDepartmentLeadBlockReason(editingDept) ?? 'This department lead cannot be changed.');
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
            showError(apiError(err, 'Failed to save department.', 'A department with this name already exists.'));
        } finally {
            setSaving(false);
        }
    };
    const deleteDept = (d: DepartmentDTO) => {
        const blockReason = getDepartmentDeleteBlockReason(d);
        if (blockReason) {
            showError(blockReason);
            return;
        }

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
                    showError(apiError(err, 'Failed to delete department.', 'This department still has members assigned and cannot be deleted.'));
                }
            },
        });
    };

    const roomTypeOptions = Object.values(RoomType).map(value => ({ label: value.replace('_', ' '), value }));
    const openCreateRoom = () => { setEditingRoom(null); setRoomForm(EMPTY_ROOM); setRoomDialog(true); };
    const openEditRoom = (room: RoomDTO) => {
        setEditingRoom(room);
        setRoomForm({
            name: room.name ?? '',
            roomType: room.roomType,
            privacyMode: room.privacyMode ?? false,
            departmentId: room.departmentId,
            buildingId: room.buildingId,
        });
        setRoomDialog(true);
    };
    const saveRoom = async () => {
        if (!roomForm.name.trim() || !roomForm.roomType || roomForm.departmentId === undefined || roomForm.buildingId === undefined) {
            showError('Please fill in all required room fields.');
            return;
        }
        setSaving(true);
        try {
            if (editingRoom?.id) {
                const dto: RoomUpdateDTO = {
                    name: roomForm.name,
                    roomType: roomForm.roomType,
                    privacyMode: roomForm.privacyMode,
                    departmentId: roomForm.departmentId,
                    buildingId: roomForm.buildingId,
                };
                const updated = await RoomService.update(editingRoom.id, dto);
                setRooms(prev => prev.map(r => r.id === updated.id ? updated : r));
                showSuccess('Room updated.');
            } else {
                const dto: RoomCreateDTO = {
                    name: roomForm.name,
                    roomType: roomForm.roomType,
                    privacyMode: roomForm.privacyMode,
                    departmentId: roomForm.departmentId,
                    buildingId: roomForm.buildingId,
                };
                const created = await RoomService.create(dto);
                setRooms(prev => [...prev, created]);
                showSuccess('Room created.');
            }
            setRoomDialog(false);
        } catch (err) {
            showError(apiError(err, 'Failed to save room.', 'A room with this name already exists.'));
        } finally {
            setSaving(false);
        }
    };
    const deleteRoom = (room: RoomDTO) => {
        const blockReason = getRoomDeleteBlockReason(room);
        if (blockReason) {
            showError(blockReason);
            return;
        }

        confirmDialog({
            message: `Delete room "${room.name}"? This will deactivate it and decommission all associated Raspberry Pis and Arduinos.`,
            header: 'Confirm Delete',
            icon: 'pi pi-trash',
            acceptClassName: 'p-button-danger',
            accept: async () => {
                try {
                    await RoomService.delete(room.id!);
                    setRooms(prev => prev.filter(r => r.id !== room.id));
                    showSuccess('Room deleted.');
                } catch (err) {
                    showError(apiError(err, 'Failed to delete room.', 'This room is still referenced and cannot be deleted.'));
                }
            },
        });
    };

    const getDepartmentMembers = (departmentId?: number) => {
        if (!departmentId) return [];
        return employeeProfiles
            .filter(profile => profile.departmentId === departmentId)
            .map(profile => ({
                profile,
                user: users.find(user => user.id === profile.userxId),
                departmentName: departmentName(profile.departmentId),
                roomName: roomName(profile.roomId),
            }));
    };

    const openAssignEmployee = async (department: DepartmentDTO) => {
        if (!department.id) return;
        setTargetDepartment(department);
        setAssignUserId(null);
        setAssignRoomId(null);
        setTargetRooms(await DepartmentService.getRooms(department.id));
        setAssignDialog(true);
    };

    const openCreateDepartmentEmployee = async (department: DepartmentDTO) => {
        if (!department.id) return;
        setTargetDepartment(department);
        setCreateEmployeeForm({ ...EMPTY_EMPLOYEE_USER, roles: new Set([UserxRole.EMPLOYEE]) });
        setCreateEmployeeValidation({ valid: true });
        setCreateEmployeeDepartmentId(department.id);
        setCreateEmployeeRoomId(undefined);
        setTargetRooms(await DepartmentService.getRooms(department.id));
        setCreateEmployeeDialog(true);
    };

    const assignExistingEmployee = async () => {
        if (!targetDepartment?.id || !assignUserId || !assignRoomId) {
            showError('Please select a user and a room.');
            return;
        }
        setSaving(true);
        try {
            const oldProfiles = employeeProfiles.filter(profile => profile.userxId === assignUserId);
            for (const profile of oldProfiles) {
                if (profile.id !== undefined) {
                    await EmployeeProfileService.delete(profile.id);
                }
            }

            const user = users.find(u => u.id === assignUserId);
            if (user) {
                const roles = new Set([...rolesToArray(user.roles), UserxRole.EMPLOYEE]);
                const update: UserxUpdateDTO = {
                    firstName: user.firstName,
                    lastName: user.lastName,
                    email: user.email,
                    phone: user.phone,
                    roles,
                };
                const updatedUser = await UserService.updateUser(assignUserId, update);
                setUsers(prev => prev.map(u => u.id === updatedUser.id ? updatedUser : u));
            }

            const createdProfile = await EmployeeProfileService.create({
                userxId: assignUserId,
                departmentId: targetDepartment.id,
                roomId: assignRoomId,
            });
            setEmployeeProfiles(prev => [
                ...prev.filter(profile => profile.userxId !== assignUserId),
                createdProfile,
            ]);
            setAssignDialog(false);
            showSuccess('Employee assigned.');
        } catch (err) {
            showError(apiError(err, 'Failed to assign employee.', 'This user already has an employee profile.'));
        } finally {
            setSaving(false);
        }
    };

    const createDepartmentEmployee = async () => {
        const fieldErrors: UserxValidationResult['fieldErrors'] = {};
        if (!createEmployeeForm.username.trim()) fieldErrors.username = 'Required';
        if (!createEmployeeForm.password.trim()) fieldErrors.password = 'Required';
        if (!createEmployeeForm.firstName?.trim()) fieldErrors.firstName = 'Required';
        if (!createEmployeeForm.lastName?.trim()) fieldErrors.lastName = 'Required';
        if (!createEmployeeForm.email?.trim()) fieldErrors.email = 'Required';
        if (!createEmployeeForm.phone?.trim()) fieldErrors.phone = 'Required';
        if (rolesToArray(createEmployeeForm.roles).length === 0) fieldErrors.roles = 'Required';
        if (createEmployeeDepartmentId === undefined) fieldErrors.departmentId = 'Required';
        if (createEmployeeRoomId === undefined) fieldErrors.roomId = 'Required';

        if (Object.keys(fieldErrors).length > 0) {
            setCreateEmployeeValidation({ valid: false, message: 'Please fill in all required fields', fieldErrors });
            return;
        }

        setSaving(true);
        try {
            const roles = new Set([...rolesToArray(createEmployeeForm.roles), UserxRole.EMPLOYEE]);
            const createdUser = await UserService.createUser({ ...createEmployeeForm, roles });
            const createdProfile = await EmployeeProfileService.create({
                userxId: createdUser.id!,
                departmentId: createEmployeeDepartmentId!,
                roomId: createEmployeeRoomId!,
            });
            setUsers(prev => [...prev, createdUser]);
            setEmployeeProfiles(prev => [...prev, createdProfile]);
            setCreateEmployeeDialog(false);
            showSuccess('Employee created and assigned.');
        } catch (err) {
            showError(apiError(err, 'Failed to create employee.', 'A user with this username already exists.'));
        } finally {
            setSaving(false);
        }
    };

    const handleCreateEmployeeInput = (event: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value } = event.target;
        setCreateEmployeeForm(prev => ({ ...prev, [name]: value }));
    };

    const handleCreateEmployeeRoles = (event: { value: string[] }) => {
        setCreateEmployeeForm(prev => ({
            ...prev,
            roles: new Set(createUserxRoleArrayFromStrings(event.value)),
        }));
    };

    const handleCreateEmployeePhone = (phone: string) => {
        setCreateEmployeeForm(prev => ({ ...prev, phone }));
    };

    const handleCreateEmployeeDepartment = async (departmentId?: number) => {
        setCreateEmployeeDepartmentId(departmentId);
        setCreateEmployeeRoomId(undefined);
        setTargetRooms([]);
        if (departmentId !== undefined) {
            setTargetRooms(await DepartmentService.getRooms(departmentId));
        }
    };

    const removeEmployeeProfile = (profile: EmployeeProfileDTO) => {
        confirmDialog({
            message: 'Remove this employee from the department?',
            header: 'Confirm Remove',
            icon: 'pi pi-trash',
            acceptClassName: 'p-button-danger',
            accept: async () => {
                try {
                    await EmployeeProfileService.delete(profile.id!);
                    setEmployeeProfiles(prev => prev.filter(p => p.id !== profile.id));
                    showSuccess('Employee profile removed.');
                } catch (err) {
                    showError(apiError(err, 'Failed to remove employee profile.'));
                }
            },
        });
    };


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
    const getDepartmentDeleteBlockReason = (department: DepartmentDTO): string | null => {
        const employeeCount = countDepartmentEmployees(department.id);
        if (employeeCount > 0) {
            return `Reassign or remove ${pluralizeEmployee(employeeCount)} from this department before deleting it.`;
        }

        if (!wouldLeaveLeadWithoutRole(department)) return null;

        const leadName = userName(department.departmentLeadId);

        return `Assign ${leadName} as lead of another department.`;
    };

    const getRoomDeleteBlockReason = (room: RoomDTO): string | null => {
        const employeeCount = countRoomEmployees(room.id);
        if (employeeCount === 0) return null;

        return `Reassign or remove ${pluralizeEmployee(employeeCount)} from this room before deleting it.`;
    };

    const blockedDeleteButton = (
        deleteButton: React.ReactNode,
        title: string,
        reason: string,
    ) => (
        <span className="blocked-delete-wrapper">
            {deleteButton}
            <span className="blocked-delete-popover" role="tooltip">
                <span className="blocked-delete-popover__title">{title}</span>
                <span className="blocked-delete-popover__text">{reason}</span>
            </span>
        </span>
    );

    const departmentActionTemplate = (department: DepartmentDTO) => {
        const deleteBlockReason = getDepartmentDeleteBlockReason(department);
        const deleteDisabled = deleteBlockReason !== null;

        const deleteButton = (
            <Button
                icon="pi pi-trash"
                size="small"
                severity="danger"
                outlined
                disabled={deleteDisabled}
                onClick={() => deleteDept(department)}
            />
        );

        return (
            <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                <Button
                    icon="pi pi-pencil"
                    size="small"
                    severity="secondary"
                    outlined
                    onClick={() => openEditDept(department)}
                />

                {deleteDisabled ? (
                    blockedDeleteButton(deleteButton, 'Department cannot be deleted', deleteBlockReason)
                ) : (
                    deleteButton
                )}
            </div>
        );
    };

    const roomActionTemplate = (room: RoomDTO) => {
        const deleteBlockReason = getRoomDeleteBlockReason(room);
        const deleteDisabled = deleteBlockReason !== null;

        const deleteButton = (
            <Button
                icon="pi pi-trash"
                size="small"
                severity="danger"
                outlined
                disabled={deleteDisabled}
                onClick={() => deleteRoom(room)}
            />
        );

        return (
            <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                <Button
                    icon="pi pi-pencil"
                    size="small"
                    severity="secondary"
                    outlined
                    onClick={() => openEditRoom(room)}
                />

                {deleteDisabled ? (
                    blockedDeleteButton(deleteButton, 'Room cannot be deleted', deleteBlockReason)
                ) : (
                    deleteButton
                )}
            </div>
        );
    };

    const filterHeader = (value: string, onChange: (value: string) => void, placeholder: string, action?: React.ReactNode) => (
        <div style={{ display: 'flex', justifyContent: 'space-between', gap: '1rem', marginBottom: '1rem', alignItems: 'center' }}>
            <span style={{ flex: '1 1 18rem', maxWidth: '28rem' }}>
                <InputText value={value} onChange={event => onChange(event.target.value)} placeholder={placeholder} style={{ width: '100%' }} />
            </span>
            {action}
        </div>
    );

    const takenAddressIds = new Set(
        buildings
            .filter(b => editingBldg == null || b.id !== editingBldg.id)
            .map(b => b.addressId)
            .filter((id): id is number => id != null)
    );
    const addressOptions = [...addresses]
        .sort((a, b) => {
            const aTaken = takenAddressIds.has(a.id!);
            const bTaken = takenAddressIds.has(b.id!);
            if (aTaken !== bTaken) return aTaken ? 1 : -1;
            return addressLabel(a.id).localeCompare(addressLabel(b.id));
        })
        .map(a => ({
            label: addressLabel(a.id),
            value: a.id,
        }));
    const addressItemTemplate = (option: { label: string; value: number }) => {
        const taken = takenAddressIds.has(option.value);
        return (
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span>{option.label}</span>
                {taken && <Tag value="Taken" severity="danger" style={{ fontSize: '0.7rem', marginLeft: '0.5rem' }} />}
            </div>
        );
    };

    const departmentAssignmentExcludedRoles: UserxRole[] = [
        UserxRole.SYSTEM_ADMIN,
        UserxRole.BUILDING_ADMIN,
        UserxRole.MANAGEMENT,
    ];
    const userOptions = users
        .filter(user => user.enabled !== false)
        .filter(user => !rolesToArray(user.roles).some(role => departmentAssignmentExcludedRoles.includes(role)))
        .map(u => ({
            label: (`${u.firstName ?? ''} ${u.lastName ?? ''}`).trim() || u.username || `User ${u.id}`,
            value: u.id,
        }));
    const takenUserIds = new Set(
        employeeProfiles.map(p => p.userxId).filter((id): id is number => id != null)
    );
    const assignUserItemTemplate = (option: { label: string; value: number }) => {
        const taken = takenUserIds.has(option.value);
        return (
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span>{option.label}</span>
                {taken && <Tag value="Taken" severity="danger" style={{ fontSize: '0.7rem', marginLeft: '0.5rem' }} />}
            </div>
        );
    };
    const departmentOptions = departments.map(d => ({ label: d.name ?? `Department ${d.id}`, value: d.id }));
    const buildingOptions = buildings.map(b => ({ label: b.name ?? `Building ${b.id}`, value: b.id }));
    const roomOptions = targetRooms.map(room => ({ label: room.name ?? `Room ${room.id}`, value: room.id }));
    const roomTypeTemplate = (row: RoomDTO) => (
        <Tag value={row.roomType?.replace('_', ' ') ?? '—'} severity={row.roomType === RoomType.OFFICE ? 'info' : 'warning'} />
    );
    const privacyTemplate = (row: RoomDTO) => (
        <Tag value={row.privacyMode ? 'On' : 'Off'} severity={row.privacyMode ? 'warning' : 'success'} />
    );
    const statusTemplate = (row: RoomDTO) => (
        <Tag value={row.active ? 'Active' : 'Inactive'} severity={row.active ? 'success' : 'danger'} />
    );
    const rolesTemplate = (user?: UserxDTO) => (
        <div style={{ display: 'flex', gap: '0.35rem', flexWrap: 'wrap' }}>
            {rolesToArray(user?.roles).map(role => <Tag key={role} value={role} />)}
        </div>
    );
    const canChangeEditingDepartmentLead = leadCanBeChanged(editingDept);

    const departmentExpansionTemplate = (department: DepartmentDTO) => {
        const members = getDepartmentMembers(department.id);
        const canRemoveProfile = (user?: UserxDTO) => {
            const roles = rolesToArray(user?.roles);
            return roles.length > 1 || !roles.includes(UserxRole.EMPLOYEE);
        };
        return (
            <div style={{ padding: '1rem 2rem', background: '#f9fafb' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', gap: '1rem', alignItems: 'center', marginBottom: '1rem' }}>
                    <h3 style={{ margin: 0, fontSize: '1rem' }}>Employees</h3>
                    <div style={{ display: 'flex', gap: '0.5rem' }}>
                        <Button label="Assign User" icon="pi pi-user-plus" size="small" outlined onClick={() => void openAssignEmployee(department)} />
                        <Button label="Create Employee" icon="pi pi-plus" size="small" onClick={() => void openCreateDepartmentEmployee(department)} />
                    </div>
                </div>
                <DataTable
                    value={members}
                    emptyMessage="No employees assigned."
                    size="small"
                >
                    <Column field="user.username" header="Username" sortable body={(row: { user?: UserxDTO }) => row.user?.username ?? '—'} />
                    <Column field="user.firstName" header="First Name" sortable body={(row: { user?: UserxDTO }) => row.user?.firstName ?? '—'} />
                    <Column field="user.lastName" header="Last Name" sortable body={(row: { user?: UserxDTO }) => row.user?.lastName ?? '—'} />
                    <Column field="roomName" header="Room" sortable />
                    <Column header="Roles" body={(row: { user?: UserxDTO }) => rolesTemplate(row.user)} />
                    <Column
                        header="Actions"
                        style={{ width: '16rem' }}
                        body={(row: { profile: EmployeeProfileDTO; user?: UserxDTO }) => {
                            if (!canRemoveProfile(row.user)) {
                                return (
                                    <span style={{ color: '#6b7280', fontSize: '0.85rem' }}>
                                        Reassign or delete this employee instead.
                                    </span>
                                );
                            }

                            return (
                                <Button
                                    icon="pi pi-trash"
                                    label="Remove Profile"
                                    size="small"
                                    severity="danger"
                                    outlined
                                    onClick={() => removeEmployeeProfile(row.profile)}
                                />
                            );
                        }}
                    />
                </DataTable>
            </div>
        );
    };

    return (
        <div>
            <NavbarComponent />
            <Toast ref={toast} />
            <ConfirmDialog />

            <style>
                {`
                    .blocked-delete-wrapper {
                        position: relative;
                        display: inline-flex;
                        cursor: help;
                    }

                    .blocked-delete-wrapper .p-button {
                        pointer-events: none;
                    }

                    .blocked-delete-popover {
                        position: absolute;
                        top: 50%;
                        right: calc(100% + 0.75rem);
                        transform: translateY(-50%) translateX(0.25rem);
                        z-index: 1000;
                        width: 19rem;
                        padding: 0.85rem 1rem;
                        border-radius: 0.75rem;
                        border: 1px solid #e5e7eb;
                        background: #ffffff;
                        box-shadow: 0 12px 30px rgba(15, 23, 42, 0.16);
                        color: #111827;
                        display: none;
                        pointer-events: none;
                        text-align: left;
                    }

                    .blocked-delete-popover::after {
                        content: "";
                        position: absolute;
                        top: 50%;
                        right: -0.45rem;
                        width: 0.85rem;
                        height: 0.85rem;
                        transform: translateY(-50%) rotate(45deg);
                        background: #ffffff;
                        border-top: 1px solid #e5e7eb;
                        border-right: 1px solid #e5e7eb;
                    }

                    .blocked-delete-wrapper:hover .blocked-delete-popover,
                    .blocked-delete-wrapper:focus-within .blocked-delete-popover {
                        display: block;
                        transform: translateY(-50%) translateX(0);
                    }

                    .blocked-delete-popover__title {
                        display: block;
                        margin-bottom: 0.35rem;
                        font-weight: 700;
                        font-size: 0.9rem;
                        color: #92400e;
                    }

                    .blocked-delete-popover__text {
                        display: block;
                        font-size: 0.82rem;
                        line-height: 1.35;
                        color: #4b5563;
                    }
                `}
            </style>

            <div style={{ padding: '1.5rem 2rem', maxWidth: '1400px', margin: '0 auto' }}>
                {/* Header */}
                <div style={{ marginBottom: '2rem', padding: '1.5rem 2rem', backgroundColor: '#f8f9fa', borderRadius: '12px', border: '1px solid #e9ecef', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
                    <h1 style={{ margin: 0, color: '#111827', fontSize: '2rem', fontWeight: 700 }}>Organization</h1>
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
                    <button style={TAB_STYLE(activeTab === 'rooms')} onClick={() => setActiveTab('rooms')}>
                        <i className="pi pi-th-large" style={{ marginRight: '0.5rem' }} />
                        Rooms
                    </button>
                </div>

                <div style={{ padding: '2rem', backgroundColor: '#ffffff', borderRadius: '0 12px 12px 12px', border: '1px solid #e5e7eb', borderTop: 'none' }}>

                    {/* Buildings Tab */}
                    {activeTab === 'buildings' && (
                        <>
                            {filterHeader(buildingFilter, setBuildingFilter, 'Filter buildings...', <Button label="Add Building" icon="pi pi-plus" onClick={openCreateBldg} />)}
                            <DataTable value={buildings} globalFilter={buildingFilter} globalFilterFields={['id', 'name', 'addressId']} stripedRows paginator rows={10} emptyMessage="No buildings found.">
                                <Column field="id" header="ID" style={{ width: '5rem' }} />
                                <Column field="name" header="Name" sortable />
                                <Column header="Address" body={(b: BuildingDTO) => addressLabel(b.addressId)} />
                                <Column header="Actions" style={{ width: '8rem' }} body={(b: BuildingDTO) => actionTemplate(() => openEditBldg(b), () => deleteBldg(b))} />
                            </DataTable>
                        </>
                    )}

                    {/* Addresses Tab */}
                    {activeTab === 'addresses' && (
                        <>
                            {filterHeader(addressFilter, setAddressFilter, 'Filter addresses...', <Button label="Add Address" icon="pi pi-plus" onClick={openCreateAddr} />)}
                            <DataTable value={addresses} globalFilter={addressFilter} globalFilterFields={['id', 'country', 'zipCode', 'city', 'street', 'houseNumber', 'extra']} stripedRows paginator rows={10} emptyMessage="No addresses found.">
                                <Column field="id" header="ID" style={{ width: '5rem' }} />
                                <Column field="country" header="Country" sortable />
                                <Column field="zipCode" header="ZIP" sortable />
                                <Column field="city" header="City" sortable />
                                <Column field="street" header="Street" sortable />
                                <Column field="houseNumber" header="No." style={{ width: '5rem' }} />
                                <Column field="extra" header="Extra" />
                                <Column header="Actions" style={{ width: '8rem' }} body={(a: AddressDTO) => actionTemplate(() => openEditAddr(a), () => deleteAddr(a))} />
                            </DataTable>
                        </>
                    )}

                    {/* Departments Tab */}
                    {activeTab === 'departments' && (
                        <>
                            {filterHeader(departmentFilter, setDepartmentFilter, 'Filter departments...', <Button label="Add Department" icon="pi pi-plus" onClick={openCreateDept} />)}
                            <DataTable
                                value={departments}
                                globalFilter={departmentFilter}
                                globalFilterFields={['id', 'name', 'departmentLeadId']}
                                stripedRows
                                paginator
                                rows={10}
                                emptyMessage="No departments found."
                                expandedRows={expandedDepartments}
                                onRowToggle={e => setExpandedDepartments(e.data as Record<string, boolean>)}
                                rowExpansionTemplate={departmentExpansionTemplate}
                                dataKey="id"
                            >
                                <Column expander style={{ width: '3rem' }} />
                                <Column field="id" header="ID" style={{ width: '5rem' }} />
                                <Column field="name" header="Name" sortable />
                                <Column header="Department Lead" body={(d: DepartmentDTO) => userName(d.departmentLeadId)} />
                                <Column header="Actions" style={{ width: '10rem' }} body={(d: DepartmentDTO) => departmentActionTemplate(d)} />
                            </DataTable>
                        </>
                    )}

                    {/* Rooms Tab */}
                    {activeTab === 'rooms' && (
                        <>
                            {filterHeader(roomFilter, setRoomFilter, 'Search rooms...', <Button label="Add Room" icon="pi pi-plus" onClick={openCreateRoom} />)}
                            <DataTable
                                value={rooms}
                                globalFilter={roomFilter}
                                globalFilterFields={['id', 'name', 'roomType', 'departmentId', 'buildingId']}
                                stripedRows
                                paginator
                                rows={10}
                                emptyMessage="No rooms found."
                            >
                                <Column field="id" header="ID" style={{ width: '5rem' }} />
                                <Column field="name" header="Name" sortable />
                                <Column field="roomType" header="Type" body={roomTypeTemplate} sortable />
                                <Column
                                    header="Department"
                                    body={(room: RoomDTO) => departmentName(room.departmentId)}
                                    sortable
                                    sortField="departmentId"
                                    sortFunction={(e) =>
                                        [...e.data].sort((a, b) =>
                                            (e.order ?? 1) *
                                            departmentName(a.departmentId).localeCompare(departmentName(b.departmentId))
                                        )
                                    }
                                />
                                <Column
                                    header="Building"
                                    body={(room: RoomDTO) => buildingName(room.buildingId)}
                                    sortable
                                    sortField="buildingId"
                                    sortFunction={(e) =>
                                        [...e.data].sort((a, b) =>
                                            (e.order ?? 1) *
                                            buildingName(a.buildingId).localeCompare(buildingName(b.buildingId))
                                        )
                                    }
                                />
                                <Column field="privacyMode" header="Privacy" body={privacyTemplate} />
                                <Column field="active" header="Status" body={statusTemplate} />
                                <Column header="Actions" style={{ width: '10rem' }} body={(room: RoomDTO) => roomActionTemplate(room)} />
                            </DataTable>
                        </>
                    )}
                </div>
            </div>

            {/* Address Dialog */}
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

            {/* Building Dialog */}
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
                            onChange={e => {
                                if (takenAddressIds.has(e.value)) {
                                    showError('This address is already assigned to another building. Choose a different address.');
                                    return;
                                }
                                setBldgForm(f => ({ ...f, addressId: e.value as number }));
                            }}
                            placeholder="Select address…"
                            filter
                            itemTemplate={addressItemTemplate}
                            style={{ width: '100%' }}
                            appendTo="self"
                        />
                    </div>
                </div>
            </Dialog>

            {/* Department Dialog */}
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
                            disabled={editingDept !== null && !canChangeEditingDepartmentLead}
                            style={{ width: '100%' }}
                            appendTo="self"
                        />
                        {editingDept !== null && !canChangeEditingDepartmentLead && (
                            <small style={{ color: '#b45309', display: 'block', marginTop: '0.4rem' }}>
                                This lead cannot be changed because this is the only department they lead and they have no other user groups.
                            </small>
                        )}
                    </div>
                </div>
            </Dialog>

            {/* Room Dialog */}
            <Dialog
                header={editingRoom ? 'Edit Room' : 'New Room'}
                visible={roomDialog}
                style={{ width: '480px' }}
                onHide={() => setRoomDialog(false)}
                footer={
                    <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'flex-end' }}>
                        <Button label="Cancel" severity="secondary" outlined onClick={() => setRoomDialog(false)} />
                        <Button label="Save" icon="pi pi-check" loading={saving} onClick={() => void saveRoom()} />
                    </div>
                }
            >
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', paddingTop: '0.5rem' }}>
                    <div>
                        <label style={{ display: 'block', fontWeight: 600, fontSize: '0.875rem', marginBottom: '0.4rem', color: '#374151' }}>Name *</label>
                        <InputText value={roomForm.name} onChange={e => setRoomForm(f => ({ ...f, name: e.target.value }))} style={{ width: '100%' }} />
                    </div>
                    <div>
                        <label style={{ display: 'block', fontWeight: 600, fontSize: '0.875rem', marginBottom: '0.4rem', color: '#374151' }}>Room Type *</label>
                        <Dropdown
                            value={roomForm.roomType}
                            options={roomTypeOptions}
                            onChange={e => setRoomForm(f => ({ ...f, roomType: e.value as RoomType }))}
                            placeholder="Select room type..."
                            style={{ width: '100%' }}
                            appendTo="self"
                        />
                    </div>
                    <div>
                        <label style={{ display: 'block', fontWeight: 600, fontSize: '0.875rem', marginBottom: '0.4rem', color: '#374151' }}>Department *</label>
                        <Dropdown
                            value={roomForm.departmentId}
                            options={departmentOptions}
                            onChange={e => setRoomForm(f => ({ ...f, departmentId: e.value as number }))}
                            placeholder="Select department..."
                            filter
                            style={{ width: '100%' }}
                            appendTo="self"
                        />
                    </div>
                    <div>
                        <label style={{ display: 'block', fontWeight: 600, fontSize: '0.875rem', marginBottom: '0.4rem', color: '#374151' }}>Building *</label>
                        <Dropdown
                            value={roomForm.buildingId}
                            options={buildingOptions}
                            onChange={e => setRoomForm(f => ({ ...f, buildingId: e.value as number }))}
                            placeholder="Select building..."
                            filter
                            style={{ width: '100%' }}
                            appendTo="self"
                        />
                    </div>
                </div>
            </Dialog>

            {/* Assign Existing User Dialog */}
            <Dialog
                header={`Assign User${targetDepartment?.name ? ` to ${targetDepartment.name}` : ''}`}
                visible={assignDialog}
                style={{ width: '460px' }}
                onHide={() => setAssignDialog(false)}
                footer={
                    <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'flex-end' }}>
                        <Button label="Cancel" severity="secondary" outlined onClick={() => setAssignDialog(false)} />
                        <Button label="Assign" icon="pi pi-check" loading={saving} onClick={() => void assignExistingEmployee()} />
                    </div>
                }
            >
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', paddingTop: '0.5rem' }}>
                    <div>
                        <label style={{ display: 'block', fontWeight: 600, fontSize: '0.875rem', marginBottom: '0.4rem', color: '#374151' }}>User *</label>
                        <Dropdown
                            value={assignUserId}
                            options={userOptions}
                            onChange={e => {
                                if (takenUserIds.has(e.value)) {
                                    showError('This user already has an employee profile. Choose a different user.');
                                    return;
                                }
                                setAssignUserId(e.value as number);
                            }}
                            placeholder="Select user..."
                            filter
                            itemTemplate={assignUserItemTemplate}
                            style={{ width: '100%' }}
                            appendTo="self"
                        />
                    </div>
                    <div>
                        <label style={{ display: 'block', fontWeight: 600, fontSize: '0.875rem', marginBottom: '0.4rem', color: '#374151' }}>Room *</label>
                        <Dropdown
                            value={assignRoomId}
                            options={roomOptions}
                            onChange={e => setAssignRoomId(e.value as number)}
                            placeholder="Select room..."
                            filter
                            style={{ width: '100%' }}
                            appendTo="self"
                        />
                    </div>
                </div>
            </Dialog>

            <UserDialog
                visible={createEmployeeDialog}
                user={createEmployeeForm}
                isNewUser
                validation={createEmployeeValidation}
                onHide={() => setCreateEmployeeDialog(false)}
                onSubmit={() => void createDepartmentEmployee()}
                onInputChange={handleCreateEmployeeInput}
                onRolesChange={handleCreateEmployeeRoles}
                onPhoneChange={handleCreateEmployeePhone}
                departments={targetDepartment ? [targetDepartment] : []}
                rooms={targetRooms}
                selectedDepartmentId={createEmployeeDepartmentId}
                selectedRoomId={createEmployeeRoomId}
                onDepartmentChange={(departmentId) => void handleCreateEmployeeDepartment(departmentId)}
                onRoomChange={setCreateEmployeeRoomId}
                lockedRoles={[UserxRole.EMPLOYEE]}
            />

        </div>
    );
};

export default OrgStructureView;

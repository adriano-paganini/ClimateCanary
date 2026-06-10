/**
 * This code is part of the skeleton project provided for students of the course "Software
 * Engineering" offered by Innsbruck University.
 */
import React, {useCallback, useEffect, useRef, useState} from 'react';

import {Button} from "primereact/button";
import {Dialog} from "primereact/dialog";
import {InputText} from "primereact/inputtext";
import {Toast} from 'primereact/toast';
import {ConfirmDialog, confirmDialog} from 'primereact/confirmdialog';
import 'primeicons/primeicons.css';

import UserListComponent from "./UserListComponent";
import UserDialog from "./UserDialog";


import {createUserxRoleArrayFromStrings, getImpliedRoles, getRoleCombinationError, hasUserRole, rolesToArray, UserxValidationResult} from '../utilities/userxUtilities';
import {
    AdminControllerApi,
    DepartmentDTO,
    EmployeeProfileDTO,
    RoomDTO,
    UserxCreateDTO,
    UserxDTO,
    UserxRole,
    UserxUpdateDTO
} from "../generated-skeleton-api";
import {useUser} from "../Contexts/AuthenticatedUserContext";
import {AuthApi} from "../utilities/authApi";
import {UserService} from "../services/UserService";
import {DepartmentService} from "../services/DepartmentService";
import {EmployeeProfileService} from "../services/EmployeeProfileService";
import {useNavigate} from "react-router-dom";
import {ROUTES} from "../utilities/routes.paths";

let cachedDepartments: DepartmentDTO[] | null = null;
let cachedDepartmentsRequest: Promise<DepartmentDTO[]> | null = null;
const cachedRoomsByDepartment = new Map<number, RoomDTO[]>();
const cachedRoomRequestsByDepartment = new Map<number, Promise<RoomDTO[]>>();

/**
 * Component for managing users.
 */
const UserTable = () => {
    const {currentUser, fullUser, logout, refreshCurrentUser} = useUser();
    const navigate = useNavigate();
    const [users, setUsers] = useState<UserxDTO[]>([]);
    const [loading, setLoading] = useState<boolean>(true);
    const [selectedUser, setSelectedUser] = useState<UserxDTO | UserxCreateDTO | null>(null);
    const [isNewUser, setIsNewUser] = useState<boolean>(false);
    const [dialogVisible, setDialogVisible] = useState<boolean>(false);
    const [selfDeleteDialogVisible, setSelfDeleteDialogVisible] = useState<boolean>(false);
    const [selfDeletePassword, setSelfDeletePassword] = useState<string>('');
    const [selfDeleteConfirmation, setSelfDeleteConfirmation] = useState<string>('');
    const [selfDeleteSubmitting, setSelfDeleteSubmitting] = useState<boolean>(false);
    const [validation, setValidation] = useState<UserxValidationResult>({valid: true});
    const [departments, setDepartments] = useState<DepartmentDTO[]>(() => cachedDepartments ?? []);
    const [departmentsLoaded, setDepartmentsLoaded] = useState<boolean>(() => cachedDepartments !== null);
    const [departmentsLoading, setDepartmentsLoading] = useState<boolean>(false);
    const [departmentRooms, setDepartmentRooms] = useState<RoomDTO[]>([]);
    const [roomsLoading, setRoomsLoading] = useState<boolean>(false);
    const [employeeProfile, setEmployeeProfile] = useState<EmployeeProfileDTO | null>(null);
    const [employeeDepartmentId, setEmployeeDepartmentId] = useState<number | undefined>(undefined);
    const [employeeRoomId, setEmployeeRoomId] = useState<number | undefined>(undefined);

    const toast = useRef<Toast | null>(null);

    /**
     * Fetch all users from the backend on mount once.
     */
    useEffect(() => {
        const fetchUsers = async () => {
            try {
                const adminControllerApi = new AdminControllerApi();
                const userxData = await adminControllerApi.getAllUsers({withCredentials: true}).then(response => response.data);
                setUsers(userxData);
            } catch (err: any) {
                console.error('Error fetching users:', err);
            } finally {
                setLoading(false);
            }
        };
        void fetchUsers();
    }, []);

    const ensureDepartmentsLoaded = useCallback(async (): Promise<DepartmentDTO[]> => {
        if (cachedDepartments !== null) {
            setDepartments(cachedDepartments);
            setDepartmentsLoaded(true);
            return cachedDepartments;
        }

        if (cachedDepartmentsRequest === null) {
            cachedDepartmentsRequest = DepartmentService.getAll()
                .then((data) => {
                    cachedDepartments = data;
                    return data;
                })
                .finally(() => {
                    cachedDepartmentsRequest = null;
                });
        }

        setDepartmentsLoading(true);
        try {
            const data = await cachedDepartmentsRequest;
            setDepartments(data);
            setDepartmentsLoaded(true);
            return data;
        } catch (err: any) {
            console.error('Error fetching departments:', err);
            toast.current?.show({severity: 'error', summary: 'Error', detail: 'Error loading departments', life: 3000});
            throw err;
        } finally {
            setDepartmentsLoading(false);
        }
    }, []);

    const hasEmployeeRole = (user: UserxDTO | UserxCreateDTO | null) =>
        hasUserRole(user, UserxRole.EMPLOYEE);

    const resetEmployeeAssignment = () => {
        setEmployeeProfile(null);
        setEmployeeDepartmentId(undefined);
        setEmployeeRoomId(undefined);
        setDepartmentRooms([]);
        setRoomsLoading(false);
    }

    const loadRoomsForDepartment = async (departmentId?: number, preferredRoomId?: number) => {
        setEmployeeDepartmentId(departmentId);
        setEmployeeRoomId(undefined);
        setDepartmentRooms([]);

        if (departmentId === undefined) return;

        const cachedRooms = cachedRoomsByDepartment.get(departmentId);
        if (cachedRooms) {
            setDepartmentRooms(cachedRooms);
            if (preferredRoomId !== undefined && cachedRooms.some(room => room.id === preferredRoomId)) {
                setEmployeeRoomId(preferredRoomId);
            }
            return;
        }

        setRoomsLoading(true);
        try {
            let request = cachedRoomRequestsByDepartment.get(departmentId);
            if (!request) {
                request = DepartmentService.getRooms(departmentId).then((rooms) => {
                    cachedRoomsByDepartment.set(departmentId, rooms);
                    return rooms;
                });
                cachedRoomRequestsByDepartment.set(departmentId, request);
            }

            const rooms = await request;
            setDepartmentRooms(rooms);
            if (preferredRoomId !== undefined && rooms.some(room => room.id === preferredRoomId)) {
                setEmployeeRoomId(preferredRoomId);
            }
        } catch (err: any) {
            console.error('Error fetching department rooms:', err);
            toast.current?.show({severity: 'error', summary: 'Error', detail: 'Error loading rooms', life: 3000});
        } finally {
            cachedRoomRequestsByDepartment.delete(departmentId);
            setRoomsLoading(false);
        }
    }

    /**
     * Validate the user object.
     */
    const validateUser = (user: UserxCreateDTO | null, opts: {
        requirePassword?: boolean
    } = {requirePassword: true}): UserxValidationResult => {

        if (!user) return {valid: false, message: 'No user selected'};

        const required: (keyof UserxCreateDTO)[] = ['firstName', 'lastName', 'username', 'email', 'phone'];
        const {requirePassword = true} = opts; // password input on edit user not needed
        const fieldErrors: UserxValidationResult['fieldErrors'] = {};

        required.forEach((k) => {
            const v = (user[k] as unknown as string) ?? '';
            if (!v.trim()) fieldErrors[k] = 'Required';
        });

        const username = ((user.username as unknown as string) ?? '').trim();
        if (username && /\s/.test(username)) {
            fieldErrors.username = 'Username must not contain whitespace';
        }

        const firstName = ((user.firstName as unknown as string) ?? '').trim();
        if (firstName.length < 1) {
            fieldErrors.firstName = 'First name is required';
        }

        const lastName = ((user.lastName as unknown as string) ?? '').trim();
        if (lastName.length < 1) {
            fieldErrors.lastName = 'Last name is required';
        }

        const email = ((user.email as unknown as string) ?? '').trim();
        const atIndex = email.indexOf('@');
        if (email && (atIndex <= 0 || !email.slice(atIndex + 1).includes('.'))) {
            fieldErrors.email = 'Enter a valid email address';
        }

        const phone = ((user.phone as unknown as string) ?? '').trim();
        if (phone && !/^\+\d{1,3}\s\d+$/.test(phone)) {
            fieldErrors.phone = 'Phone must include a country code and digits only';
        }

        // check for password required
        const pwd = (user.password as unknown as string) ?? '';
        if (requirePassword && !pwd.trim()) fieldErrors.password = 'Required';

        // at least one role required (see also UserxCreateDTO in backend
        const selectedRoles = rolesToArray(user.roles);
        if (selectedRoles.length === 0) {
            fieldErrors.roles = 'Required';
        }

        const combinationError = getRoleCombinationError(selectedRoles);
        if (combinationError) {
            fieldErrors.roles = combinationError;
        }

        if (selectedRoles.includes(UserxRole.DEPARTMENT_LEAD) && !selectedRoles.includes(UserxRole.EMPLOYEE)) {
            fieldErrors.roles = 'Department Lead must also have the Employee role.';
        }

        if (hasEmployeeRole(user)) {
            if (employeeDepartmentId === undefined) {
                fieldErrors.departmentId = 'Required';
            }
            if (employeeRoomId === undefined) {
                fieldErrors.roomId = 'Required';
            }
        }

        const valid = Object.keys(fieldErrors).length === 0;
        return valid
            ? {valid}
            : {valid, message: 'Please fill in all required fields', fieldErrors};
    }

    /**
     * Handle the submit event for the user dialog.
     */
    const handleSubmit = async () => {
        const validationResult = validateUser(selectedUser as UserxCreateDTO | null, {requirePassword: isNewUser});
        if (!validationResult.valid) {
            // Display an error eventMessage or handle the validation error
            setValidation(validationResult);
            console.error('Please fill in all required fields.');
            return;
        }

        setValidation({valid: true});

        if (isNewUser) {
            await createUser();
        } else {
            await updateUser();
        }
        hideDialog();
    };

    /**
     * Create a new user and update the state.
     */
    const createUser = async () => {
        if (!selectedUser) return;

        // assert type of selectedUser to UserxCreateDTO
        const userToCreate = {...selectedUser, enabled: true} as UserxCreateDTO;

        if (userToCreate.password === undefined) {
            return;
        }

        try {
            const adminControllerAPI = new AdminControllerApi();
            const newUser = await adminControllerAPI.createUser({userxCreateDTO: userToCreate}).then(response => response.data);
            await syncEmployeeProfile(newUser, userToCreate);
            setUsers([...users, newUser as UserxDTO]);
        } catch (err: any) {
            console.error('Error saving user:', err);
            toast.current?.show({severity: 'error', summary: 'Error', detail: 'Error saving user', life: 3000});
        }
    }

    /**
     * Update an existing user and update the state.
     */
    const updateUser = async () => {
        if (!selectedUser) return;

        // assert type of selectedUser to UserxDTO
        const userToUpdate = selectedUser as UserxDTO;

        if (userToUpdate.id === undefined) return;

        try {
            const adminControllerAPI = new AdminControllerApi();
            const userxUpdateDTO: UserxUpdateDTO = {
                email: userToUpdate.email,
                roles: userToUpdate.roles,
                firstName: userToUpdate.firstName,
                lastName: userToUpdate.lastName,
                phone: userToUpdate.phone,
            };

            const updatedUser = await adminControllerAPI.updateUser({
                userxUpdateDTO,
                id: userToUpdate.id
            }).then(response => response.data);
            await syncEmployeeProfile(updatedUser, userToUpdate);
            setUsers(users.map((user: UserxDTO) => user.id === updatedUser.id ? updatedUser : user));
            if (!isNewUser && isCurrentUser(updatedUser)) {
                await refreshCurrentUser();
            }
        } catch (err: any) {
            console.error('Error updating user:', err);
            toast.current?.show({severity: 'error', summary: 'Error', detail: 'Error updating user', life: 3000});
        }
    }

    const syncEmployeeProfile = async (savedUser: UserxDTO, sourceUser: UserxDTO | UserxCreateDTO) => {
        if (!savedUser.id) return;

        const existingProfiles = await EmployeeProfileService.getAll(savedUser.id);
        const existingProfile = employeeProfile ?? existingProfiles.find(profile => profile.id !== undefined) ?? null;

        if (!hasEmployeeRole(sourceUser)) {
            for (const profile of existingProfiles) {
                if (profile.id !== undefined) {
                    await EmployeeProfileService.delete(profile.id);
                }
            }
            if (existingProfile?.id !== undefined && !existingProfiles.some(profile => profile.id === existingProfile.id)) {
                await EmployeeProfileService.delete(existingProfile.id);
            }
            if (existingProfiles.length > 0 || existingProfile?.id !== undefined) {
                setEmployeeProfile(null);
                setEmployeeDepartmentId(undefined);
                setEmployeeRoomId(undefined);
                setDepartmentRooms([]);
            }
            return;
        }

        if (employeeDepartmentId === undefined || employeeRoomId === undefined) return;

        const oldProfiles = existingProfiles.filter(profile => profile.id !== undefined);
        for (const profile of oldProfiles) {
            await EmployeeProfileService.delete(profile.id!);
        }
        if (oldProfiles.length > 0) {
            await UserService.updateUser(savedUser.id, {
                firstName: savedUser.firstName,
                lastName: savedUser.lastName,
                email: savedUser.email,
                phone: savedUser.phone,
                roles: new Set([...rolesToArray(savedUser.roles), UserxRole.EMPLOYEE]),
            });
        }

        const createdProfile = await EmployeeProfileService.create({
            userxId: savedUser.id,
            departmentId: employeeDepartmentId,
            roomId: employeeRoomId,
        });
        setEmployeeProfile(createdProfile);
    }

    /**
     * Delete a user and update the state.
     */
    const deleteUser = async (user: UserxDTO) => {
        if (!user.id) return;

        try {
            const adminControllerAPI = new AdminControllerApi();
            await adminControllerAPI.deleteUser({id: user.id});
            setUsers(users.map(u => u.id === user.id ? {...u, enabled: false} : u));
            toast.current?.show({severity: 'success', summary: 'Deleted', detail: `User "${user.username}" deleted`, life: 3000});
        } catch (err: any) {
            console.error('Error deleting user:', err);
            toast.current?.show({severity: 'error', summary: 'Error', detail: 'Error deleting user', life: 3000});
        }
    }

    const resetSelfDeleteDialog = () => {
        setSelfDeletePassword('');
        setSelfDeleteConfirmation('');
        setSelfDeleteSubmitting(false);
    }

    const showSelfDeleteDialog = () => {
        resetSelfDeleteDialog();
        setSelfDeleteDialogVisible(true);
    }

    const hideSelfDeleteDialog = () => {
        setSelfDeleteDialogVisible(false);
        resetSelfDeleteDialog();
    }

    const deleteCurrentUser = async () => {
        const username = fullUser?.username ?? currentUser?.username;
        if (!username) return;

        if (selfDeleteConfirmation !== 'DELETE ME') {
            toast.current?.show({severity: 'warn', summary: 'Confirmation required', detail: 'Type DELETE ME to confirm deletion.', life: 3000});
            return;
        }

        setSelfDeleteSubmitting(true);
        try {
            await AuthApi.login({username, password: selfDeletePassword});
            await UserService.deleteCurrentUser();
            logout();
            navigate(ROUTES.LOGIN, {replace: true});
        } catch (err: any) {
            console.error('Error deleting current user:', err);
            toast.current?.show({severity: 'error', summary: 'Error', detail: 'Password confirmation failed or user could not be deleted.', life: 3000});
            setSelfDeleteSubmitting(false);
        }
    }

    const restoreUser = async (user: UserxDTO) => {
        if (!user.id) return;

        try {
            const adminControllerAPI = new AdminControllerApi();
            const restoredUser = await adminControllerAPI.updateUser({
                userxUpdateDTO: {...user, enabled: true} as UserxUpdateDTO,
                id: user.id
            }).then(response => response.data);
            setUsers(users.map(u => u.id === restoredUser.id ? restoredUser : u));
            toast.current?.show({severity: 'success', summary: 'Restored', detail: `User "${user.username}" restored`, life: 3000});
        } catch (err: any) {
            console.error('Error restoring user:', err);
            toast.current?.show({severity: 'error', summary: 'Error', detail: 'Error restoring user', life: 3000});
        }
    }

    /**
     * Show a confirmation dialog before deleting a user.
     */
    const confirmDeleteUser = (user: UserxDTO) => {
        if (isCurrentUser(user)) {
            showSelfDeleteDialog();
            return;
        }

        confirmDialog({
            message: `Are you sure you want to delete user "${user.username}"?`,
            header: 'Confirm Delete',
            icon: 'pi pi-exclamation-triangle',
            acceptClassName: 'p-button-danger',
            accept: () => deleteUser(user),
        });
    };

    /**
     * Open the edit dialog for a user.
     */
    const openEditDialog = (user: UserxDTO) => {
        setSelectedUser(user);
        setValidation({valid: true});
        setIsNewUser(false);
        resetEmployeeAssignment();
        showDialog();

        if (user.id && hasEmployeeRole(user)) {
            void Promise.all([
                ensureDepartmentsLoaded(),
                EmployeeProfileService.getAll(user.id),
            ])
                .then(async (profiles) => {
                    const profile = profiles[1][0] ?? null;
                    setEmployeeProfile(profile);
                    if (profile?.departmentId !== undefined) {
                        await loadRoomsForDepartment(profile.departmentId, profile.roomId);
                    }
                })
                .catch((err: any) => {
                    console.error('Error fetching employee profile:', err);
                    toast.current?.show({severity: 'error', summary: 'Error', detail: 'Error loading employee profile', life: 3000});
                });
        }
    };

    /**
     * Open the dialog for creating a new user.
     */
    const openNewUserDialog = () => {
        const newUser: UserxCreateDTO = {
            username: '',
            firstName: '',
            lastName: '',
            email: '',
            phone: '',
            enabled: true,
            roles: new Set(),
            password: ''
        };
        setSelectedUser(newUser);
        resetEmployeeAssignment();
        showDialog()
        setIsNewUser(true);
    }

    const showDialog = () => {
        setValidation({valid: true});
        setDialogVisible(true);
    }

    const hideDialog = () => {
        setValidation({valid: true});
        setDialogVisible(false);
    };

    const handleInputChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        if (!selectedUser) return;

        const {name, value} = event.target;

        setSelectedUser({...selectedUser, [name]: value});
    }

    const handlePhoneChange = (phone: string) => {
        if (!selectedUser) return;

        setSelectedUser({...selectedUser, phone});
    }

    const handleRolesChange = (event: { value: string[] }) => {
        if (!selectedUser) return;

        const roles = createUserxRoleArrayFromStrings(event.value);
        const allRoles = Array.from(new Set([...roles, ...getImpliedRoles(roles)]));

        setSelectedUser({...selectedUser, roles: new Set(allRoles)});
        if (allRoles.includes(UserxRole.EMPLOYEE)) {
            void ensureDepartmentsLoaded();
        }
        if (!allRoles.includes(UserxRole.EMPLOYEE)) {
            resetEmployeeAssignment();
        }
    }

    const handleEmployeeDepartmentChange = (departmentId?: number) => {
        void loadRoomsForDepartment(departmentId);
    }

    const handleEmployeeRoomChange = (roomId?: number) => {
        setEmployeeRoomId(roomId);
    }

    const activeUsers = users.filter(user => user.enabled !== false);
    const deletedUsers = users.filter(user => user.enabled === false);
    const currentUserId = fullUser?.id;
    const currentUsername = fullUser?.username ?? currentUser?.username;
    const isCurrentUser = (user: UserxDTO) =>
        (currentUserId !== undefined && user.id === currentUserId) ||
        (currentUsername !== undefined && user.username === currentUsername);
    const canSubmitSelfDelete = selfDeletePassword.length > 0 && selfDeleteConfirmation === 'DELETE ME' && !selfDeleteSubmitting;
    const lockedRoles = !isNewUser
        && selectedUser
        && isCurrentUser(selectedUser as UserxDTO)
        && hasUserRole(selectedUser, UserxRole.SYSTEM_ADMIN)
        ? [UserxRole.SYSTEM_ADMIN]
        : [];

    const selfDeleteFooter = (
        <div>
            <Button label="Cancel" icon="pi pi-times" text onClick={hideSelfDeleteDialog} disabled={selfDeleteSubmitting}/>
            <Button
                label="Delete my account"
                icon="pi pi-trash"
                severity="danger"
                onClick={() => void deleteCurrentUser()}
                disabled={!canSubmitSelfDelete}
                loading={selfDeleteSubmitting}
            />
        </div>
    );

    return (<>
            <Toast ref={toast}/>
            <ConfirmDialog/>

            <div style={{ padding: '1.5rem 2rem', maxWidth: '1400px', margin: '0 auto' }}>
                {/* Header */}
                <div style={{ marginBottom: '2rem', padding: '1.5rem 2rem', backgroundColor: '#f8f9fa', borderRadius: '12px', border: '1px solid #e9ecef', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
                    <h1 style={{ margin: 0, color: '#111827', fontSize: '2rem', fontWeight: 700 }}>User Management</h1>
                </div>

                {/* Content */}
                <div style={{ padding: '2rem', backgroundColor: '#ffffff', borderRadius: '12px', border: '1px solid #e5e7eb' }}>
                    <Button label="Add User" icon="pi pi-plus" className="p-button-raised p-button-rounded"
                            style={{marginBottom: "10px"}} onClick={openNewUserDialog}/>

                    <section style={{marginTop: "1rem"}}>
                        <h2 style={{fontSize: "1.15rem", margin: "0 0 0.75rem"}}>Active Users</h2>
                        <UserListComponent
                            users={activeUsers}
                            loading={loading}
                            onEditUser={openEditDialog}
                            onDeleteUser={confirmDeleteUser}
                            currentUserId={currentUserId}
                            currentUsername={currentUsername}
                            emptyMessage="No active users found"
                        />
                    </section>

                    <section style={{marginTop: "2rem"}}>
                        <h2 style={{fontSize: "1.15rem", margin: "0 0 0.75rem"}}>Deleted Users</h2>
                        <UserListComponent
                            users={deletedUsers}
                            loading={loading}
                            onEditUser={openEditDialog}
                            onDeleteUser={confirmDeleteUser}
                            showDeleteAction={false}
                            onRestoreUser={restoreUser}
                            currentUserId={currentUserId}
                            currentUsername={currentUsername}
                            emptyMessage="No deleted users found"
                        />
                    </section>
                </div>
            </div>

            <UserDialog visible={dialogVisible} user={selectedUser} isNewUser={isNewUser} validation={validation}
                        onHide={hideDialog} onSubmit={handleSubmit}
                        onInputChange={handleInputChange} onRolesChange={handleRolesChange}
                        onPhoneChange={handlePhoneChange}
                        disableUsername={!isNewUser}
                        departments={departments}
                        rooms={departmentRooms}
                        selectedDepartmentId={employeeDepartmentId}
                        selectedRoomId={employeeRoomId}
                        onDepartmentChange={handleEmployeeDepartmentChange}
                        onRoomChange={handleEmployeeRoomChange}
                        departmentsLoading={departmentsLoading}
                        roomsLoading={roomsLoading}
                        lockedRoles={lockedRoles}/>
            <Dialog
                header="Delete your account"
                visible={selfDeleteDialogVisible}
                style={{width: '32rem', maxWidth: '95vw'}}
                modal
                footer={selfDeleteFooter}
                onHide={hideSelfDeleteDialog}
                closable={!selfDeleteSubmitting}
            >
                <div style={{display: 'flex', flexDirection: 'column', gap: '1rem'}}>
                    <p style={{margin: 0}}>
                        You are deleting your own account. Confirm your password and type <strong>DELETE ME</strong> to continue.
                    </p>
                    <div>
                        <label htmlFor="self-delete-password" className="font-bold block" style={{marginBottom: '0.35rem'}}>Password</label>
                        <InputText
                            id="self-delete-password"
                            type="password"
                            value={selfDeletePassword}
                            onChange={(event) => setSelfDeletePassword(event.target.value)}
                            disabled={selfDeleteSubmitting}
                            style={{width: '100%'}}
                            autoComplete="current-password"
                        />
                    </div>
                    <div>
                        <label htmlFor="self-delete-confirmation" className="font-bold block" style={{marginBottom: '0.35rem'}}>Confirmation</label>
                        <InputText
                            id="self-delete-confirmation"
                            value={selfDeleteConfirmation}
                            onChange={(event) => setSelfDeleteConfirmation(event.target.value)}
                            disabled={selfDeleteSubmitting}
                            placeholder="DELETE ME"
                            style={{width: '100%'}}
                        />
                    </div>
                </div>
            </Dialog>
        </>
    );
};

export default UserTable;

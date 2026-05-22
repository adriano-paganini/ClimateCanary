/**
 * This code is part of the skeleton project provided for students of the course "Software
 * Engineering" offered by Innsbruck University.
 */
import React, {useEffect, useRef, useState} from 'react';

import {Button} from "primereact/button";
import {Card} from 'primereact/card';
import {Dialog} from "primereact/dialog";
import {InputText} from "primereact/inputtext";
import {InputMaskChangeEvent} from "primereact/inputmask";
import {Toast} from 'primereact/toast';
import {ConfirmDialog, confirmDialog} from 'primereact/confirmdialog';
import 'primeicons/primeicons.css';

import UserListComponent from "./UserListComponent";
import UserDialog from "./UserDialog";


import {createUserxRoleArrayFromStrings, UserxValidationResult} from '../utilities/userxUtilities';
import {CheckboxChangeEvent} from "primereact/checkbox";
import {AdminControllerApi, UserxCreateDTO, UserxDTO, UserxUpdateDTO} from "../generated-skeleton-api";
import {useUser} from "../Contexts/AuthenticatedUserContext";
import {AuthApi} from "../utilities/authApi";
import {UserService} from "../services/UserService";
import {useNavigate} from "react-router-dom";
import {ROUTES} from "../utilities/routes.paths";

/**
 * Component for managing users.
 */
const UserTable = () => {
    const {currentUser, fullUser, logout} = useUser();
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

    /**
     * Validate the user object.
     */
    const validateUser = (user: UserxCreateDTO | null, opts: {
        requirePassword?: boolean
    } = {requirePassword: true}): UserxValidationResult => {

        if (!user) return {valid: false, message: 'No user selected'};

        const required: (keyof UserxCreateDTO)[] = ['firstName', 'lastName', 'username'];
        const {requirePassword = true} = opts; // password input on edit user not needed
        const fieldErrors: Partial<Record<keyof UserxCreateDTO, string>> = {};

        required.forEach((k) => {
            const v = (user[k] as unknown as string) ?? '';
            if (!v.trim()) fieldErrors[k] = 'Required';
        });

        // check for password required
        const pwd = (user.password as unknown as string) ?? '';
        if (requirePassword && !pwd.trim()) fieldErrors.password = 'Required';

        // at least one role required (see also UserxCreateDTO in backend
        if (!user.roles || user.roles.size === 0) {
            fieldErrors.roles = 'Required';
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
        const userToCreate = selectedUser as UserxCreateDTO;

        if (userToCreate.password === undefined) {
            return;
        }

        try {
            const adminControllerAPI = new AdminControllerApi();
            const newUser = await adminControllerAPI.createUser({userxCreateDTO: userToCreate}).then(response => response.data);
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
            const updatedUser = await adminControllerAPI.updateUser({
                userxUpdateDTO: userToUpdate as UserxUpdateDTO,
                id: userToUpdate.id
            }).then(response => response.data);
            setUsers(users.map((user: UserxDTO) => user.id === updatedUser.id ? updatedUser : user));
        } catch (err: any) {
            console.error('Error updating user:', err);
            toast.current?.show({severity: 'error', summary: 'Error', detail: 'Error updating user', life: 3000});
        }
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
        showDialog()
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

    const handleInputChange = (event: React.ChangeEvent<HTMLInputElement> | InputMaskChangeEvent) => {
        if (!selectedUser) return;

        const {name, value} = event.target;

        setSelectedUser({...selectedUser, [name]: value});
    }

    const handleUserEnabledChange = (event: CheckboxChangeEvent) => {
        if (!selectedUser) return;

        const {name, checked} = event.target;

        setSelectedUser({...selectedUser, [name]: checked});
    }

    const handleRolesChange = (event: { value: string[] }) => {
        if (!selectedUser) return;

        const roles = createUserxRoleArrayFromStrings(event.value);

        setSelectedUser({...selectedUser, roles: new Set(roles)});
    }

    const activeUsers = users.filter(user => user.enabled !== false);
    const deletedUsers = users.filter(user => user.enabled === false);
    const currentUserId = fullUser?.id;
    const currentUsername = fullUser?.username ?? currentUser?.username;
    const isCurrentUser = (user: UserxDTO) =>
        (currentUserId !== undefined && user.id === currentUserId) ||
        (currentUsername !== undefined && user.username === currentUsername);
    const canSubmitSelfDelete = selfDeletePassword.length > 0 && selfDeleteConfirmation === 'DELETE ME' && !selfDeleteSubmitting;

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

    return (<Card title="User Management" className="m-4">
            <Toast ref={toast}/>
            <ConfirmDialog/>
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

            <UserDialog visible={dialogVisible} user={selectedUser} isNewUser={isNewUser} validation={validation}
                        onHide={hideDialog} onSubmit={handleSubmit}
                        onInputChange={handleInputChange} onRolesChange={handleRolesChange}
                        onUserEnabledChange={handleUserEnabledChange}/>
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
        </Card>
    );
};

export default UserTable;

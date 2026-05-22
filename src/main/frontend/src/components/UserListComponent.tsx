/**
 * This code is part of the skeleton project provided for students of the course "Software
 * Engineering" offered by Innsbruck University.
 */
import React from "react";

import {Button} from "primereact/button";
import {Column} from "primereact/column";
import {DataTable} from "primereact/datatable";
import {Tag} from "primereact/tag";

import {rolesBodyTemplate} from "./rolesBodyTemplate";
import {UserxDTO} from "../generated-skeleton-api";

interface UserListProps {
    users: UserxDTO[];
    loading: boolean;
    onEditUser: (user: UserxDTO) => void;
    onDeleteUser: (user: UserxDTO) => void;
    showDeleteAction?: boolean;
    onRestoreUser?: (user: UserxDTO) => void;
    currentUsername?: string;
    currentUserId?: number;
    emptyMessage?: string;
}


/**
 * Component for displaying a list of users in a DataTable.
 * @param users the users to display
 * @param loading whether the users are loading
 * @param onEditUser callback when a user is edited
 * @param onDeleteUser callback when a user is deleted
 */
const UserListComponent: React.FC<UserListProps> = ({
    users,
    loading,
    onEditUser,
    onDeleteUser,
    showDeleteAction = true,
    onRestoreUser,
    currentUsername,
    currentUserId,
    emptyMessage = "No users found",
}) => {

    const isCurrentUser = (user: UserxDTO) =>
        (currentUserId !== undefined && user.id === currentUserId) ||
        (currentUsername !== undefined && user.username === currentUsername);

    const usernameTemplate = (rowData: UserxDTO) => (
        <div style={{display: "flex", alignItems: "center", gap: "0.5rem"}}>
            <span>{rowData.username}</span>
            {isCurrentUser(rowData) && <Tag value="You" severity="success"/>}
        </div>
    );

    const editButtonTemplate = (rowData: UserxDTO) => (
        <Button
            label="Change"
            icon="pi pi-external-link"
            onClick={() => onEditUser(rowData)}
            aria-label={`Change ${rowData.username}`}
        />
    );

    const deleteButtonTemplate = (rowData: UserxDTO) => (
        <Button
            label="Delete"
            icon="pi pi-trash"
            severity="danger"
            onClick={() => onDeleteUser(rowData)}
            aria-label={`Delete ${rowData.username}`}
        />
    );

    const restoreButtonTemplate = (rowData: UserxDTO) => (
        <Button
            label="Restore"
            icon="pi pi-refresh"
            severity="success"
            onClick={() => onRestoreUser?.(rowData)}
            aria-label={`Restore ${rowData.username}`}
        />
    );

    return (
        <DataTable value={users} loading={loading} emptyMessage={emptyMessage}>
            <Column field="username" header="Username" body={usernameTemplate} sortable/>
            <Column field="firstName" header="First Name" sortable/>
            <Column field="lastName" header="Last Name" sortable/>
            <Column field="roles" header="Roles" body={rolesBodyTemplate}/>
            <Column body={editButtonTemplate} exportable={false} style={{minWidth: '8rem'}}/>
            {showDeleteAction && <Column body={deleteButtonTemplate} exportable={false} style={{minWidth: '8rem'}}/>}
            {onRestoreUser && <Column body={restoreButtonTemplate} exportable={false} style={{minWidth: '8rem'}}/>}
        </DataTable>
    );
};

export default UserListComponent;

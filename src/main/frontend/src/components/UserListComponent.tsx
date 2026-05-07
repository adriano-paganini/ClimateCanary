/**
 * This code is part of the skeleton project provided for students of the course "Software
 * Engineering" offered by Innsbruck University.
 */
import React from "react";

import {Button} from "primereact/button";
import {Column} from "primereact/column";
import {DataTable} from "primereact/datatable";

import {Checkbox} from "primereact/checkbox";
import {rolesBodyTemplate} from "./rolesBodyTemplate";
import {UserxDTO} from "../generated-skeleton-api";

interface UserListProps {
    users: UserxDTO[];
    loading: boolean;
    onEditUser: (user: UserxDTO) => void;
    onDeleteUser: (user: UserxDTO) => void;
}


/**
 * Component for displaying a list of users in a DataTable.
 * @param users the users to display
 * @param loading whether the users are loading
 * @param onEditUser callback when a user is edited
 * @param onDeleteUser callback when a user is deleted
 */
const UserListComponent: React.FC<UserListProps> = ({users, loading, onEditUser, onDeleteUser}) => {

    const editButtonTemplate = (rowData: UserxDTO) => (
        <Button
            label="Details"
            icon="pi pi-external-link"
            onClick={() => onEditUser(rowData)}
            aria-label={`Details for ${rowData.username}`}
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

    const enableButtonTemplate = (rowData: UserxDTO) => (
        <Checkbox checked={rowData.enabled ?? false} disabled={true} className="p-mr-2"/>
    );


    return (
        <DataTable value={users} loading={loading}>
            <Column field="username" header="Username" sortable/>
            <Column field="firstName" header="First Name" sortable/>
            <Column field="lastName" header="Last Name" sortable/>
            <Column field="roles" header="Roles" body={rolesBodyTemplate}/>
            <Column field="enabled" header="Enabled" body={enableButtonTemplate}/>
            <Column body={editButtonTemplate} exportable={false} style={{minWidth: '8rem'}}/>
            <Column body={deleteButtonTemplate} exportable={false} style={{minWidth: '8rem'}}/>
        </DataTable>
    );
};

export default UserListComponent;

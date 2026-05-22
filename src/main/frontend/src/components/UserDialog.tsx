/**
 * This code is part of the skeleton project provided for students of the course "Software
 * Engineering" offered by Innsbruck University.
 */
import React from 'react';
import {Dialog} from 'primereact/dialog';
import {Button} from "primereact/button";

import UserForm from './UserForm';
import {UserxValidationResult} from "../utilities/userxUtilities";
import {Message} from "primereact/message";
import {UserxCreateDTO, UserxDTO} from "../generated-skeleton-api";

interface UserDialogProps {
    visible: boolean;
    user: UserxDTO | UserxCreateDTO | null;
    isNewUser: boolean;
    validation: UserxValidationResult;
    onHide: () => void;
    onSubmit: () => void;
    onInputChange: (event: React.ChangeEvent<HTMLInputElement>) => void;
    onRolesChange: (event: { value: string[] }) => void;
    onPhoneChange: (phone: string) => void;
    disableUsername?: boolean;
}

/**
 * Dialog for creating or editing a user.
 * @param visible whether the dialog is visible
 * @param user the user to be edited
 * @param isNewUser whether the user is new
 * @param validation field validation information
 * @param onHide callback when the dialog is hidden
 * @param onSubmit callback when the user is submitted
 * @param onInputChange callback when the input changes
 * @param onRolesChange callback when the roles change
 * @param onPhoneChange callback when the phone number changes
 */
const UserDialog: React.FC<UserDialogProps> = ({
                                                   visible,
                                                   user,
                                                   isNewUser,
                                                   validation,
                                                   onHide,
                                                   onSubmit,
                                                   onInputChange,
                                                   onRolesChange,
                                                   onPhoneChange,
                                                   disableUsername = false
                                               }) => {

    /**
     * Renders the footer of the dialog.
     */
    const renderFooter = () => (
        <div>
            <Button label="Cancel" icon="pi pi-times" onClick={onHide} className="p-button-text"/>
            <Button label={isNewUser ? "Create" : "Save"} icon="pi pi-check" onClick={onSubmit}
                    autoFocus/>
        </div>
    );

    return (
        <Dialog
            header={isNewUser ? "Create New User" : "Edit User"}
            visible={visible}
            style={{width: '50vw'}}
            onHide={onHide}
            footer={renderFooter}
        >
            <div className="user-dialog-scroll-body" onWheelCapture={(event) => event.stopPropagation()}>
                {validation.message && (<Message severity="error" text={validation.message} className="mb-3"/>)}
                {user && (
                    <UserForm
                        user={user}
                        isNewUser={isNewUser}
                        fieldErrors={validation.fieldErrors}
                        onInputChange={onInputChange}
                        onRolesChange={onRolesChange}
                        onPhoneChange={onPhoneChange}
                        disableUsername={disableUsername}
                    />
                )}
            </div>
        </Dialog>
    );
};

export default UserDialog;

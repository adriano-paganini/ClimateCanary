import React, { useEffect, useState } from 'react';
import { Button } from 'primereact/button';
import { Dropdown } from 'primereact/dropdown';
import { InputText } from 'primereact/inputtext';
import { Message } from 'primereact/message';
import { ProgressSpinner } from 'primereact/progressspinner';
import { Tag } from 'primereact/tag';
import { Divider } from 'primereact/divider';
import { Toast } from 'primereact/toast';
import 'primeicons/primeicons.css';

import NavbarComponent from '../components/NavbarComponent';
import { COUNTRY_CODE_OPTIONS } from '../components/UserForm';
import { useUser } from '../Contexts/AuthenticatedUserContext';
import { UserService } from '../services/UserService';
import { UserxDTO, UserxRole } from '../generated-skeleton-api';

const ROLE_LABELS: Record<string, string> = {
    [UserxRole.SYSTEM_ADMIN]:   'System Admin',
    [UserxRole.BUILDING_ADMIN]: 'Building Admin',
    [UserxRole.DEPARTMENT_LEAD]: 'Department Lead',
    [UserxRole.MANAGEMENT]:     'Management',
    [UserxRole.EMPLOYEE]:       'Employee',
};

const ROLE_SEVERITY: Record<string, string> = {
    [UserxRole.SYSTEM_ADMIN]:   'danger',
    [UserxRole.BUILDING_ADMIN]: 'warning',
    [UserxRole.DEPARTMENT_LEAD]: 'info',
    [UserxRole.MANAGEMENT]:     'secondary',
    [UserxRole.EMPLOYEE]:       'success',
};

const UserProfileView: React.FC = () => {
    const { fullUser, refreshCurrentUser } = useUser();
    const toastRef = React.useRef<Toast>(null);
    
    const [user, setUser] = useState<UserxDTO | null>(null);
    const [loading, setLoading] = useState(true);

    const [editing, setEditing] = useState(false);
    const [firstName, setFirstName] = useState('');
    const [lastName, setLastName] = useState('');
    const [email, setEmail] = useState('');
    const [phone, setPhone] = useState('');

    const [saving, setSaving] = useState(false);
    const [saveError, setSaveError] = useState<string | null>(null);
    const [fieldErrors, setFieldErrors] = useState<Partial<Record<'firstName' | 'lastName' | 'email' | 'phone', string>>>({});

    useEffect(() => {
        if (fullUser) {
            setUser(fullUser);
            setFirstName(fullUser.firstName ?? '');
            setLastName(fullUser.lastName ?? '');
            setEmail(fullUser.email ?? '');
            setPhone(fullUser.phone ?? '');
            setLoading(false);
        }
    }, [fullUser]);

    const handleEdit = () => {
        setSaveError(null);
        setFieldErrors({});
        setEditing(true);
    };

    const handleCancel = () => {
        setFirstName(user?.firstName ?? '');
        setLastName(user?.lastName ?? '');
        setEmail(user?.email ?? '');
        setPhone(user?.phone ?? '');
        setSaveError(null);
        setFieldErrors({});
        setEditing(false);
    };

    const validateProfile = () => {
        const errors: Partial<Record<'firstName' | 'lastName' | 'email' | 'phone', string>> = {};
        const trimmedFirstName = firstName.trim();
        const trimmedLastName = lastName.trim();
        const trimmedEmail = email.trim();
        const trimmedPhone = phone.trim();

        if (!trimmedFirstName) errors.firstName = 'Required';
        if (!trimmedLastName) errors.lastName = 'Required';
        if (!trimmedEmail) errors.email = 'Required';
        if (!trimmedPhone) errors.phone = 'Required';

        const atIndex = trimmedEmail.indexOf('@');
        if (trimmedEmail && (atIndex <= 0 || !trimmedEmail.slice(atIndex + 1).includes('.'))) {
            errors.email = 'Enter a valid email address';
        }

        if (trimmedPhone && !/^\+\d{1,3}\s\d+$/.test(trimmedPhone)) {
            errors.phone = 'Phone must include a country code and digits only';
        }

        setFieldErrors(errors);
        return Object.keys(errors).length === 0;
    };

    const handleSave = async () => {
        setSaveError(null);
        if (!validateProfile()) {
            setSaveError('Please fix the highlighted fields.');
            return;
        }
        setSaving(true);
        try {
            const updated = await UserService.updateCurrentUser({
                firstName: firstName.trim(),
                lastName: lastName.trim(),
                email: email.trim(),
                phone: phone.trim(),
            });
            setUser(updated);
            await refreshCurrentUser();
            setFieldErrors({});
            setEditing(false);
            toastRef.current?.show({ 
                severity: 'success', 
                summary: 'Success', 
                detail: 'Profile updated successfully!',
                life: 3000
            });
        } catch (err) {
            const errorMsg = err instanceof Error ? err.message : String(err);
            setSaveError(errorMsg);
            toastRef.current?.show({ 
                severity: 'error', 
                summary: 'Error', 
                detail: errorMsg,
                life: 5000
            });
        } finally {
            setSaving(false);
        }
    };

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

    if (!user) {
        return (
            <div>
                <NavbarComponent />
                <div style={{ padding: '1.5rem 2rem' }}>
                    <Message severity="error" text="Failed to load profile." />
                </div>
            </div>
        );
    }

    const roles = user.roles ? [...user.roles] : [];
    const initials = `${user.firstName?.[0] ?? ''}${user.lastName?.[0] ?? ''}`.toUpperCase();
    const selectedCountry = COUNTRY_CODE_OPTIONS.find(option => phone.startsWith(option.value))?.value ?? '+43';
    const localPhone = phone.startsWith(selectedCountry)
        ? phone.slice(selectedCountry.length).replace(/\s/g, '')
        : phone.replace(/^\+\d+\s*/, '').replace(/\s/g, '');
    const updatePhone = (countryCode: string, localNumber: string) => {
        setPhone(localNumber ? `${countryCode} ${localNumber}` : countryCode);
    };

    return (
        <div>
            <NavbarComponent />
            <Toast ref={toastRef} position="top-right" />
            
            <div style={{ padding: '2rem' }}>
                <h2 style={{ margin: '0 0 1.5rem', color: '#111827', fontSize: '1.875rem', fontWeight: 700 }}>My Profile</h2>

                {/* Main Card */}
                <div style={{
                    background: '#fff',
                    borderRadius: '12px',
                    border: '1px solid rgba(226, 232, 240, 0.5)',
                    boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.05)',
                    overflow: 'hidden',
                    marginBottom: '1.5rem'
                }}>
                    {/* Header */}
                    <div style={{
                        padding: '1.5rem',
                        borderBottom: '1px solid #f3f4f6',
                        background: '#fafbfc'
                    }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                            <div style={{
                                width: '64px', height: '64px', borderRadius: '50%',
                                background: '#0369a1', color: '#fff',
                                display: 'flex', alignItems: 'center', justifyContent: 'center',
                                fontSize: '1.5rem', fontWeight: 700, flexShrink: 0,
                                boxShadow: '0 4px 12px rgba(3, 105, 161, 0.3)'
                            }}>
                                {initials || <i className="pi pi-user" />}
                            </div>
                            <div style={{ flex: 1 }}>
                                <div style={{ fontWeight: 700, fontSize: '1.25rem', color: '#111827' }}>
                                    {user.firstName} {user.lastName}
                                </div>
                                <div style={{ color: '#6b7280', fontSize: '0.875rem', marginTop: '0.25rem' }}>@{user.username}</div>
                            </div>
                            {!editing && (
                                <Button
                                    icon="pi pi-pencil"
                                    label="Edit"
                                    className="p-button-outlined p-button-sm"
                                    onClick={handleEdit}
                                />
                            )}
                        </div>
                    </div>

                    {/* Content */}
                    <div style={{ padding: '1.5rem' }}>
                        {saveError && (
                            <div style={{ marginBottom: '1.5rem' }}>
                                <Message severity="error" text={saveError} />
                            </div>
                        )}

                        {editing ? (
                            <div>
                                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.5rem', marginBottom: '1.5rem' }}>
                                    <div>
                                        <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, color: '#374151', marginBottom: '0.5rem' }}>First Name</label>
                                        <InputText value={firstName} onChange={e => setFirstName(e.target.value)} className={fieldErrors.firstName ? 'p-invalid' : undefined} style={{ width: '100%' }} />
                                        {fieldErrors.firstName && <small className="p-error">{fieldErrors.firstName}</small>}
                                    </div>
                                    <div>
                                        <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, color: '#374151', marginBottom: '0.5rem' }}>Last Name</label>
                                        <InputText value={lastName} onChange={e => setLastName(e.target.value)} className={fieldErrors.lastName ? 'p-invalid' : undefined} style={{ width: '100%' }} />
                                        {fieldErrors.lastName && <small className="p-error">{fieldErrors.lastName}</small>}
                                    </div>
                                </div>
                                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.5rem', marginBottom: '1.5rem' }}>
                                    <div>
                                        <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, color: '#374151', marginBottom: '0.5rem' }}>Email</label>
                                        <InputText value={email} onChange={e => setEmail(e.target.value)} className={fieldErrors.email ? 'p-invalid' : undefined} style={{ width: '100%' }} />
                                        {fieldErrors.email && <small className="p-error">{fieldErrors.email}</small>}
                                    </div>
                                    <div>
                                        <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, color: '#374151', marginBottom: '0.5rem' }}>Phone</label>
                                        <div style={{ display: 'grid', gridTemplateColumns: '12rem minmax(10rem, 1fr)', gap: '0.5rem' }}>
                                            <Dropdown
                                                value={selectedCountry}
                                                options={COUNTRY_CODE_OPTIONS}
                                                onChange={(event) => updatePhone(event.value, localPhone)}
                                                filter
                                                optionLabel="label"
                                                filterBy="search,label,value"
                                                scrollHeight="14rem"
                                                className={fieldErrors.phone ? 'p-invalid' : undefined}
                                            />
                                            <InputText
                                                value={localPhone}
                                                onChange={(event) => updatePhone(selectedCountry, event.target.value.replace(/\D/g, ''))}
                                                placeholder="123456789"
                                                keyfilter="int"
                                                className={fieldErrors.phone ? 'p-invalid' : undefined}
                                                style={{ width: '100%' }}
                                            />
                                        </div>
                                        {fieldErrors.phone && <small className="p-error">{fieldErrors.phone}</small>}
                                    </div>
                                </div>
                                <div style={{ display: 'flex', gap: '0.75rem', paddingTop: '1rem' }}>
                                    <Button
                                        label="Save"
                                        icon="pi pi-check"
                                        loading={saving}
                                        onClick={() => void handleSave()}
                                        style={{ background: '#111827', border: 'none' }}
                                    />
                                    <Button
                                        label="Cancel"
                                        icon="pi pi-times"
                                        className="p-button-outlined"
                                        onClick={handleCancel}
                                    />
                                </div>
                            </div>
                        ) : (
                            <div>
                                {/* Information Grid */}
                                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '2rem', marginBottom: '2rem' }}>
                                    <div>
                                        <p style={{ margin: '0 0 0.5rem', fontSize: '0.85rem', fontWeight: 600, color: '#6b7280' }}>First Name</p>
                                        <p style={{ margin: 0, fontSize: '1rem', color: '#111827', fontWeight: 500 }}>
                                            {firstName || <span style={{ color: '#d1d5db' }}>Not provided</span>}
                                        </p>
                                    </div>
                                    <div>
                                        <p style={{ margin: '0 0 0.5rem', fontSize: '0.85rem', fontWeight: 600, color: '#6b7280' }}>Last Name</p>
                                        <p style={{ margin: 0, fontSize: '1rem', color: '#111827', fontWeight: 500 }}>
                                            {lastName || <span style={{ color: '#d1d5db' }}>Not provided</span>}
                                        </p>
                                    </div>
                                </div>

                                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '2rem' }}>
                                    <div>
                                        <p style={{ margin: '0 0 0.5rem', fontSize: '0.85rem', fontWeight: 600, color: '#6b7280' }}>Email</p>
                                        <p style={{ margin: 0, fontSize: '1rem', color: '#111827', fontWeight: 500 }}>
                                            {email || <span style={{ color: '#d1d5db' }}>Not provided</span>}
                                        </p>
                                    </div>
                                    <div>
                                        <p style={{ margin: '0 0 0.5rem', fontSize: '0.85rem', fontWeight: 600, color: '#6b7280' }}>Phone</p>
                                        <p style={{ margin: 0, fontSize: '1rem', color: '#111827', fontWeight: 500 }}>
                                            {phone || <span style={{ color: '#d1d5db' }}>Not provided</span>}
                                        </p>
                                    </div>
                                </div>

                                {/* Divider */}
                                <Divider style={{ margin: '2rem 0' }} />

                                {/* Roles Section */}
                                <div>
                                    <h3 style={{ margin: '0 0 1rem', color: '#111827', fontSize: '1rem', fontWeight: 700 }}>Account Roles</h3>
                                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.75rem' }}>
                                        {roles.length > 0 ? (
                                            roles.map(role => (
                                                <Tag
                                                    key={role}
                                                    value={ROLE_LABELS[role] ?? role}
                                                    severity={ROLE_SEVERITY[role] as any}
                                                />
                                            ))
                                        ) : (
                                            <span style={{ color: '#d1d5db', fontSize: '0.9rem' }}>No roles assigned</span>
                                        )}
                                    </div>
                                </div>
                            </div>
                        )}
                    </div>
                </div>

            </div>
        </div>
    );
};

export default UserProfileView;

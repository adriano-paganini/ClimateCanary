import React, { useEffect, useState } from 'react';
import { Button } from 'primereact/button';
import { InputText } from 'primereact/inputtext';
import { Message } from 'primereact/message';
import { ProgressSpinner } from 'primereact/progressspinner';
import { Tag } from 'primereact/tag';
import 'primeicons/primeicons.css';

import NavbarComponent from '../components/NavbarComponent';
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

const cardStyle: React.CSSProperties = {
    background: '#fff',
    border: '1px solid #e5e7eb',
    borderRadius: '8px',
    padding: '1.5rem',
    marginBottom: '1.5rem',
    maxWidth: '600px',
};

const labelStyle: React.CSSProperties = {
    display: 'block',
    fontSize: '0.85rem',
    fontWeight: 600,
    color: '#374151',
    marginBottom: '0.35rem',
};

const fieldRowStyle: React.CSSProperties = {
    display: 'flex',
    flexDirection: 'column',
    gap: '1rem',
    marginBottom: '1.25rem',
};

const UserProfileView: React.FC = () => {
    const { fullUser, refreshCurrentUser } = useUser();
    const [user, setUser] = useState<UserxDTO | null>(null);
    const [loading, setLoading] = useState(true);

    const [editing, setEditing] = useState(false);
    const [firstName, setFirstName] = useState('');
    const [lastName, setLastName] = useState('');
    const [email, setEmail] = useState('');
    const [phone, setPhone] = useState('');

    const [saving, setSaving] = useState(false);
    const [saveError, setSaveError] = useState<string | null>(null);
    const [saveSuccess, setSaveSuccess] = useState(false);

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
        setSaveSuccess(false);
        setEditing(true);
    };

    const handleCancel = () => {
        setFirstName(user?.firstName ?? '');
        setLastName(user?.lastName ?? '');
        setEmail(user?.email ?? '');
        setPhone(user?.phone ?? '');
        setSaveError(null);
        setEditing(false);
    };

    const handleSave = async () => {
        setSaveError(null);
        setSaveSuccess(false);
        setSaving(true);
        try {
            const updated = await UserService.updateCurrentUser({
                firstName,
                lastName,
                email,
                phone,
            });
            setUser(updated);
            await refreshCurrentUser();
            setSaveSuccess(true);
            setEditing(false);
        } catch (err) {
            setSaveError(err instanceof Error ? err.message : String(err));
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
                <div className="m-4">
                    <Message severity="error" text="Failed to load profile." />
                </div>
            </div>
        );
    }

    const roles = user.roles ? [...user.roles] : [];
    const initials = `${user.firstName?.[0] ?? ''}${user.lastName?.[0] ?? ''}`.toUpperCase();

    return (
        <div>
            <NavbarComponent />
            <div style={{ padding: '1.5rem 2rem' }}>
                <h2 style={{ margin: '0 0 1.5rem', color: '#111827' }}>My Profile</h2>

                <div style={cardStyle}>
                    {/* Avatar + name header */}
                    <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '1.5rem', paddingBottom: '1.25rem', borderBottom: '1px solid #f3f4f6' }}>
                        <div style={{
                            width: '56px', height: '56px', borderRadius: '50%',
                            background: '#0369a1', color: '#fff',
                            display: 'flex', alignItems: 'center', justifyContent: 'center',
                            fontSize: '1.3rem', fontWeight: 700, flexShrink: 0,
                        }}>
                            {initials || <i className="pi pi-user" />}
                        </div>
                        <div>
                            <div style={{ fontWeight: 700, fontSize: '1.1rem', color: '#111827' }}>
                                {user.firstName} {user.lastName}
                            </div>
                            <div style={{ color: '#6b7280', fontSize: '0.9rem' }}>@{user.username}</div>
                        </div>
                        {!editing && (
                            <Button
                                icon="pi pi-pencil"
                                label="Edit"
                                className="p-button-outlined p-button-sm"
                                onClick={handleEdit}
                                style={{ marginLeft: 'auto' }}
                            />
                        )}
                    </div>

                    {saveSuccess && (
                        <div style={{ marginBottom: '1rem' }}>
                            <Message severity="success" text="Profile updated successfully." />
                        </div>
                    )}
                    {saveError && (
                        <div style={{ marginBottom: '1rem' }}>
                            <Message severity="error" text={saveError} />
                        </div>
                    )}

                    {editing ? (
                        <div>
                            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '1rem' }}>
                                <div>
                                    <label style={labelStyle}>First Name</label>
                                    <InputText value={firstName} onChange={e => setFirstName(e.target.value)} style={{ width: '100%' }} />
                                </div>
                                <div>
                                    <label style={labelStyle}>Last Name</label>
                                    <InputText value={lastName} onChange={e => setLastName(e.target.value)} style={{ width: '100%' }} />
                                </div>
                            </div>
                            <div style={fieldRowStyle}>
                                <div>
                                    <label style={labelStyle}>Email</label>
                                    <InputText value={email} onChange={e => setEmail(e.target.value)} style={{ width: '100%' }} />
                                </div>
                                <div>
                                    <label style={labelStyle}>Phone</label>
                                    <InputText value={phone} onChange={e => setPhone(e.target.value)} style={{ width: '100%' }} />
                                </div>
                            </div>
                            <div style={{ display: 'flex', gap: '0.75rem' }}>
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
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                                <div>
                                    <span style={labelStyle}>First Name</span>
                                    <span style={{ color: '#111827' }}>{user.firstName || '—'}</span>
                                </div>
                                <div>
                                    <span style={labelStyle}>Last Name</span>
                                    <span style={{ color: '#111827' }}>{user.lastName || '—'}</span>
                                </div>
                            </div>
                            <div>
                                <span style={labelStyle}>Email</span>
                                <span style={{ color: '#111827' }}>{user.email || '—'}</span>
                            </div>
                            <div>
                                <span style={labelStyle}>Phone</span>
                                <span style={{ color: '#111827' }}>{user.phone || '—'}</span>
                            </div>
                            <div>
                                <span style={labelStyle}>Roles</span>
                                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem', marginTop: '0.25rem' }}>
                                    {roles.map(role => (
                                        <Tag key={role} value={ROLE_LABELS[role] ?? role} severity="info" />
                                    ))}
                                </div>
                            </div>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default UserProfileView;

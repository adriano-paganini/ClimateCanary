import {ROUTES} from "../utilities/routes.paths";
import {UserxRole} from "../generated-skeleton-api";

export type MenuItemConfig = {
    label: string;
    icon?: string;
    route?: string;
    roles?: UserxRole[];
    items?: MenuItemConfig[];
};

export const menuConfig: MenuItemConfig[] = [
    // --- Employee & Department Lead: own room dashboard + absences ---
    {
        label: 'Dashboard', icon: 'pi pi-home', route: ROUTES.DASHBOARD,
        roles: [UserxRole.EMPLOYEE, UserxRole.DEPARTMENT_LEAD],
    },
    {
        label: 'Absences', icon: 'pi pi-calendar', route: ROUTES.ABSENCES,
        roles: [UserxRole.EMPLOYEE, UserxRole.DEPARTMENT_LEAD],
    },

    // --- Department Lead: department overview + alerts ---
    {
        label: 'Department', icon: 'pi pi-building',
        roles: [UserxRole.DEPARTMENT_LEAD],
        items: [
            {label: 'Overview', icon: 'pi pi-chart-bar', route: ROUTES.DEPARTMENT_DASHBOARD, roles: [UserxRole.DEPARTMENT_LEAD]},
            {label: 'Alerts', icon: 'pi pi-exclamation-triangle', route: ROUTES.DEPARTMENT_ALERTS, roles: [UserxRole.DEPARTMENT_LEAD]},
        ],
    },

    // --- Management: company-wide aggregated dashboard ---
    {
        label: 'Management', icon: 'pi pi-chart-line', route: ROUTES.MANAGEMENT_DASHBOARD,
        roles: [UserxRole.MANAGEMENT],
    },

    // --- Building Admin (Facility Manager): thresholds + rooms + buildings ---
    {
        label: 'Configuration', icon: 'pi pi-cog',
        roles: [UserxRole.BUILDING_ADMIN],
        items: [
            {label: 'Thresholds', icon: 'pi pi-sliders-h', route: ROUTES.THRESHOLDS, roles: [UserxRole.BUILDING_ADMIN]},
            {label: 'Rooms', icon: 'pi pi-th-large', route: ROUTES.ROOMS, roles: [UserxRole.BUILDING_ADMIN]},
            {label: 'Buildings', icon: 'pi pi-building', route: ROUTES.BUILDINGS, roles: [UserxRole.BUILDING_ADMIN]},
        ],
    },

    // --- System Admin: users + buildings/org + devices ---
    {
        label: 'Administration', icon: 'pi pi-shield',
        roles: [UserxRole.SYSTEM_ADMIN],
        items: [
            {label: 'Users', icon: 'pi pi-users', route: ROUTES.USERS, roles: [UserxRole.SYSTEM_ADMIN]},
            {label: 'Buildings & Org', icon: 'pi pi-building', route: ROUTES.BUILDINGS, roles: [UserxRole.SYSTEM_ADMIN]},
            {label: 'Devices', icon: 'pi pi-desktop', route: ROUTES.DEVICES, roles: [UserxRole.SYSTEM_ADMIN]},
        ],
    },

    // --- All authenticated users ---
    {label: 'Profile', icon: 'pi pi-user', route: ROUTES.PROFILE},
    {label: 'Logout', icon: 'pi pi-sign-out', route: ROUTES.LOGOUT},
];
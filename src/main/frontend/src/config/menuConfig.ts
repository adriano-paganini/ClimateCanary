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
    {
        label: 'Home', icon: 'pi pi-home', route: ROUTES.HOME,
    },
    {
        label: 'Dashboard', icon: 'pi pi-chart-bar', route: ROUTES.DASHBOARD,
        roles: [UserxRole.EMPLOYEE, UserxRole.DEPARTMENT_LEAD, UserxRole.MANAGEMENT],
    },
    {
        label: 'Abwesenheiten', icon: 'pi pi-calendar', route: ROUTES.ABSENCE,
        roles: [UserxRole.EMPLOYEE, UserxRole.DEPARTMENT_LEAD],
    },
    {
        label: 'Räume', icon: 'pi pi-building', route: ROUTES.ROOMS,
        roles: [UserxRole.BUILDING_ADMIN],
    },
    {
        label: 'Schwellenwerte', icon: 'pi pi-sliders-h', route: ROUTES.THRESHOLDS,
        roles: [UserxRole.BUILDING_ADMIN],
    },
    {
        label: 'Geräte', icon: 'pi pi-server', route: ROUTES.DEVICES,
        roles: [UserxRole.BUILDING_ADMIN, UserxRole.SYSTEM_ADMIN],
    },
    {
        label: 'Benutzerverwaltung', icon: 'pi pi-users', route: ROUTES.MANAGE_USERS,
        roles: [UserxRole.BUILDING_ADMIN, UserxRole.SYSTEM_ADMIN],
    },
    {
        label: 'Gebäude', icon: 'pi pi-map', route: ROUTES.BUILDINGS,
        roles: [UserxRole.SYSTEM_ADMIN],
    },
    {
        label: 'Abteilungen', icon: 'pi pi-sitemap', route: ROUTES.DEPARTMENTS,
        roles: [UserxRole.SYSTEM_ADMIN],
    },
    {
        label: 'Logout', icon: 'pi pi-sign-out', route: ROUTES.LOGOUT,
    },
];

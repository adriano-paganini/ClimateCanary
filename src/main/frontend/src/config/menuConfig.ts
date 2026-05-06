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
        label: 'Absences', icon: 'pi pi-calendar', route: ROUTES.ABSENCE,
        roles: [UserxRole.EMPLOYEE, UserxRole.DEPARTMENT_LEAD],
    },
    {
        label: 'Department', icon: 'pi pi-sitemap', route: ROUTES.DEPARTMENT_DASHBOARD,
        roles: [UserxRole.DEPARTMENT_LEAD],
    },
    {
        label: 'Rooms', icon: 'pi pi-building', route: ROUTES.ROOMS,
        roles: [UserxRole.BUILDING_ADMIN],
    },
    {
        label: 'Thresholds', icon: 'pi pi-sliders-h', route: ROUTES.THRESHOLDS,
        roles: [UserxRole.BUILDING_ADMIN],
    },
    {
        label: 'Devices', icon: 'pi pi-server', route: ROUTES.DEVICES,
        roles: [UserxRole.BUILDING_ADMIN, UserxRole.SYSTEM_ADMIN],
    },
    {
        label: 'User Management', icon: 'pi pi-users', route: ROUTES.MANAGE_USERS,
        roles: [UserxRole.BUILDING_ADMIN, UserxRole.SYSTEM_ADMIN],
    },
    {
        label: 'Buildings', icon: 'pi pi-map', route: ROUTES.BUILDINGS,
        roles: [UserxRole.SYSTEM_ADMIN],
    },
    {
        label: 'Departments', icon: 'pi pi-sitemap', route: ROUTES.DEPARTMENTS,
        roles: [UserxRole.SYSTEM_ADMIN],
    },
    {
        label: 'Logout', icon: 'pi pi-sign-out', route: ROUTES.LOGOUT,
    },
];

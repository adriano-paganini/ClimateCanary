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
        label: 'ClimateCanary', route: ROUTES.HOME,
    },
    {
        label: 'Dashboard', icon: 'pi pi-chart-bar', route: ROUTES.DASHBOARD,
        roles: [UserxRole.EMPLOYEE],
    },
    {
        label: 'Absences', icon: 'pi pi-calendar', route: ROUTES.ABSENCE,
        roles: [UserxRole.EMPLOYEE],
    },
    {
        label: 'Department', icon: 'pi pi-sitemap', route: ROUTES.DEPARTMENT_DASHBOARD,
        roles: [UserxRole.DEPARTMENT_LEAD],
    },
    {
        label: 'Team Absences', icon: 'pi pi-calendar-times', route: ROUTES.DEPARTMENT_ABSENCES,
        roles: [UserxRole.DEPARTMENT_LEAD],
    },
    {
        label: 'Overview', icon: 'pi pi-chart-bar', route: ROUTES.MANAGEMENT_DASHBOARD,
        roles: [UserxRole.MANAGEMENT],
    },
    {
        label: 'Building', icon: 'pi pi-building', roles: [UserxRole.BUILDING_ADMIN],
        items: [
            { label: 'Rooms',      icon: 'pi pi-building',  route: ROUTES.ROOMS,      roles: [UserxRole.BUILDING_ADMIN] },
            { label: 'Thresholds', icon: 'pi pi-sliders-h', route: ROUTES.THRESHOLDS, roles: [UserxRole.BUILDING_ADMIN] },
        ],
    },
    {
        label: 'Admin', icon: 'pi pi-cog', roles: [UserxRole.SYSTEM_ADMIN],
        items: [
            { label: 'Devices',         icon: 'pi pi-server', route: ROUTES.DEVICES,      roles: [UserxRole.SYSTEM_ADMIN] },
            { label: 'User Management', icon: 'pi pi-users',  route: ROUTES.MANAGE_USERS, roles: [UserxRole.SYSTEM_ADMIN] },
            { label: 'Organization',    icon: 'pi pi-map',    route: ROUTES.BUILDINGS,    roles: [UserxRole.SYSTEM_ADMIN] },
        ],
    },
];

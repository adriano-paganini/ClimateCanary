export const ROUTES = {
    HOME: '/',
    MANAGE_USERS: '/manage-users',
    LOGIN: '/login',
    LOGOUT: '/logout',
    PROFILE: '/profile',
    // Employee
    DASHBOARD: '/dashboard',
    DASHBOARD_HISTORY: '/dashboard/:roomId/history',
    ABSENCES: '/absences',
    // Department Lead
    DEPARTMENT_DASHBOARD: '/department',
    DEPARTMENT_ALERTS: '/department/alerts',
    // Management
    MANAGEMENT_DASHBOARD: '/management',
    // Building Admin (Facility Manager)
    THRESHOLDS: '/admin/thresholds',
    ROOMS: '/admin/rooms',
    BUILDINGS: '/admin/buildings',
    // System Admin
    USERS: '/admin/users',
    DEVICES: '/admin/devices',
} as const;
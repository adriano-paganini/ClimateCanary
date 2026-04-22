export const ROUTES = {
    HOME: '/',
    MANAGE_USERS: '/manage-users',
    LOGIN: '/login',
    LOGOUT: '/logout',
    DASHBOARD: '/dashboard',
    DASHBOARD_HISTORY: '/dashboard/:roomId/history',
    // Landing pages per role (full route list is expanded in issue #10)
    DEPARTMENT_DASHBOARD: '/department',
    MANAGEMENT_DASHBOARD: '/management',
    THRESHOLDS: '/admin/thresholds',
    USERS: '/admin/users',
} as const;
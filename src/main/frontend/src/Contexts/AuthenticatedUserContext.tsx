/**
 * This code is part of the skeleton project provided for students of the course "Software
 * Engineering" offered by Innsbruck University.
 */
import React, {createContext, useCallback, useContext, useEffect, useMemo, useState} from 'react';
import {BEARER_TOKEN_LOCAL_STORAGE_KEY} from "../config/config";
import {jwtDecode, JwtPayload} from "jwt-decode";
import {LoginRequestDTO, UserxDTO, UserxRole} from '../generated-skeleton-api';
import {AuthApi} from "../utilities/authApi";
import {UserService} from "../services/UserService";
import {invalidateDashboardCaches} from "../utilities/dashboardCacheInvalidation";


interface UserContextType {
    currentUser: UserxDTO | null;
    fullUser: UserxDTO | null;
    refreshCurrentUser: () => Promise<void>;
    login: (loginRequestDTO: LoginRequestDTO) => Promise<void>;
    logout: () => void;
    error: Error | null;
    isSystemAdmin: boolean;
    isBuildingAdmin: boolean;
    isDeptLead: boolean;
    isManagement: boolean;
    isEmployee: boolean;
    userIsAuthenticated: () => Promise<boolean>;
}

export const UserContext = createContext<UserContextType | null>(null);

type CustomJwtPayload = JwtPayload & { roles: string[], name: string, username: string, email: string }

export function UserProvider({children}: { children: React.ReactNode }) {

    const [error, setError] = useState<Error | null>(null);
    const [fullUser, setFullUser] = useState<UserxDTO | null>(null);

    const [token, setToken] = useState<string | null>(() => {
        return localStorage.getItem(BEARER_TOKEN_LOCAL_STORAGE_KEY);
    });

    // keep tabs/windows in sync on login/logout
    useEffect(() => {
        const handler = (e: StorageEvent) => {
            if (e.key === BEARER_TOKEN_LOCAL_STORAGE_KEY) {
                invalidateDashboardCaches();
                setToken(e.newValue);
            }
        };

        window.addEventListener("storage", handler);
        return () => window.removeEventListener("storage", handler);
    }, []);

    const normalizeUser = (user: UserxDTO): UserxDTO => ({
        ...user,
        roles: new Set([...(Array.isArray(user.roles) ? user.roles : user.roles ?? [])] as UserxRole[]),
    });

    const refreshCurrentUser = useCallback(async () => {
        try {
            const user = await UserService.getCurrentUser();
            setFullUser(normalizeUser(user));
        } catch {
            setFullUser(null);
        }
    }, []);

    useEffect(() => {
        if (token) {
            void refreshCurrentUser();
        } else {
            setFullUser(null);
        }
    }, [token, refreshCurrentUser]);

    const login = async (loginDto: LoginRequestDTO): Promise<void> => {
        const {bearerToken} = await AuthApi.login(loginDto);
        if (!bearerToken || bearerToken.length < 10) {
            setError(new Error('Missing or invalid bearer token in response!'));
            return;
        }

        invalidateDashboardCaches();
        localStorage.setItem(BEARER_TOKEN_LOCAL_STORAGE_KEY, bearerToken);
        setToken(bearerToken);
        setError(null);
    };

    const logout = async () => {
        invalidateDashboardCaches();
        localStorage.removeItem(BEARER_TOKEN_LOCAL_STORAGE_KEY);
        setToken(null);
    };

    const tokenUser = useMemo<UserxDTO | null>(() => {
        if (!token) {
            return null;
        }

        try {
            const decoded = jwtDecode<CustomJwtPayload>(token);

            const fullName = decoded.name ?? "";
            const [firstName = "", lastName = ""] = fullName.split(" ");
            const roles = decoded.roles ?? [];
            return {
                username: decoded.username ?? "",
                firstName,
                lastName,
                email: decoded.email ?? "",
                phone: "",
                enabled: true,
                roles: new Set(roles.map((role) => role as UserxRole)),
            };
        } catch {
            return null;
        }
    }, [token]);

    const currentUser = fullUser ?? tokenUser;

    // JWT is signed by the backend — if valid and not expired, the user is authenticated.
    // Actual API calls will be rejected by the backend if the token is invalid.
    const userIsAuthenticated = async (): Promise<boolean> => {
        if (!token) {
            return false;
        }

        try {
            const decoded = jwtDecode<CustomJwtPayload>(token);

            if (decoded.exp && Date.now() >= decoded.exp * 1000) {
                console.info("JWT Token expired");
                void logout();
                return false;
            }

            return true;
        } catch {
            void logout();
            return false;
        }
    };

    const roles = currentUser?.roles ?? new Set<UserxRole>();
    const isSystemAdmin   = roles.has(UserxRole.SYSTEM_ADMIN);
    const isBuildingAdmin = roles.has(UserxRole.BUILDING_ADMIN);
    const isDeptLead      = roles.has(UserxRole.DEPARTMENT_LEAD);
    const isManagement    = roles.has(UserxRole.MANAGEMENT);
    const isEmployee      = roles.has(UserxRole.EMPLOYEE);

    return (
        <UserContext.Provider
            value={{
                currentUser,
                fullUser,
                refreshCurrentUser,
                login,
                logout,
                error,
                isSystemAdmin,
                isBuildingAdmin,
                isDeptLead,
                isManagement,
                isEmployee,
                userIsAuthenticated
            }}
        >
            {children}
        </UserContext.Provider>
    );
}

export function useUser() {
    const context = useContext(UserContext);
    if (!context) {
        throw new Error('useUser must be used within a UserProvider');
    }
    return context;
}

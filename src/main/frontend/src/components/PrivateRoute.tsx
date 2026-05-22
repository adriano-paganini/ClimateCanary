/**
 * This code is part of the skeleton project provided for students of the course "Software
 * Engineering" offered by Innsbruck University.
 */
import {Navigate, Outlet, useLocation} from 'react-router-dom';
import {useEffect, useState} from 'react';

import {useUser} from "../Contexts/AuthenticatedUserContext";
import {UserxRole} from "../generated-skeleton-api";
import {ROUTES} from "../utilities/routes.paths";
import LoadingScreen from "./LoadingScreen";

/**
 * Private route component that checks if the user is authenticated. Used to protect routes.
 */
const PrivateRoute = ({roles}: { roles?: UserxRole[] }) => {

    enum AuthStatus {
        AUTHENTICATED = 200,
        UNAUTHENTICATED = 401,
        UNKNOWN = 0
    }

    const {userIsAuthenticated, currentUser} = useUser();
    const location = useLocation();

    const [authStatus, setAuthStatus] = useState(AuthStatus.UNKNOWN);

    // eslint-disable-next-line react-hooks/exhaustive-deps
    useEffect(() => {
        const checkAuthentication = async () => {
            try {
                const isAuthenticated = await userIsAuthenticated();

                if (isAuthenticated) {
                    setAuthStatus(AuthStatus.AUTHENTICATED);
                } else {
                    setAuthStatus(AuthStatus.UNAUTHENTICATED);
                }
            } catch (err: any) {
                console.warn('Backend not available:', err);
                setAuthStatus(AuthStatus.UNAUTHENTICATED);
            }
        };
        void checkAuthentication(); // to mark the returned Promise as explicitly and intentionally unawaited (ESLint)
    }, []); // an empty dependency array signals that the effect is executed only once on mount

    // loading spinner
    if (authStatus === AuthStatus.UNKNOWN) {
        return <LoadingScreen/>
    }

    if (authStatus === AuthStatus.UNAUTHENTICATED) {
        return <Navigate to={ROUTES.LOGIN} replace state={{from: location}}/>;
    }

    if (roles && roles.length > 0) {
        const hasRequiredRole = roles.some(r => currentUser?.roles?.has(r));
        if (!hasRequiredRole) return <Navigate to={ROUTES.HOME} replace/>;
    }

    return <Outlet/>;
};

export default PrivateRoute;

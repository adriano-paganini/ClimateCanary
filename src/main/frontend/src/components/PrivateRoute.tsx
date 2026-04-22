/**
 * This code is part of the skeleton project provided for students of the course "Software
 * Engineering" offered by Innsbruck University.
 */
import {Navigate, Outlet, useLocation} from 'react-router-dom';
import {useEffect, useState} from 'react';
import {ProgressSpinner} from 'primereact/progressspinner';

import {useUser} from "../Contexts/AuthenticatedUserContext";
import {UserxRole} from "../generated-skeleton-api";
import {ROUTES} from "../utilities/routes.paths";

/**
 * Private route component that checks authentication and optionally enforces role-based access.
 * Usage without roles: all authenticated users may enter.
 * Usage with roles: only authenticated users with at least one matching role may enter.
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
        void checkAuthentication();
    }, []); // an empty dependency array signals that the effect is executed only once on mount

    // loading spinner while auth status is being determined
    if (authStatus === AuthStatus.UNKNOWN) {
        return <ProgressSpinner/>
    }

    if (authStatus === AuthStatus.UNAUTHENTICATED) {
        return <Navigate to={ROUTES.LOGIN} replace state={{from: location}}/>;
    }

    // If specific roles are required, check whether the current user has at least one of them.
    // Redirect to HOME (not login) so the user stays logged in but sees their own landing page.
    if (roles && roles.length > 0) {
        const hasRequiredRole = roles.some(r => currentUser?.roles?.has(r));
        if (!hasRequiredRole) {
            return <Navigate to={ROUTES.HOME} replace/>;
        }
    }

    return <Outlet/>;
};

export default PrivateRoute;

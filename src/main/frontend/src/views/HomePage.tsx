import {useEffect, useState} from "react";
import {Navigate} from "react-router-dom";
import {useUser} from "../Contexts/AuthenticatedUserContext";
import {UserxRole} from "../generated-skeleton-api";
import {ROUTES} from "../utilities/routes.paths";
import LoadingScreen from "../components/LoadingScreen";

/**
 * The logo/home route should not be a placeholder page. Send users to the
 * most relevant implemented view for their role.
 */
const HomePage = () => {
    const {currentUser, refreshCurrentUser} = useUser();
    const [refreshing, setRefreshing] = useState(true);

    useEffect(() => {
        let active = true;
        refreshCurrentUser().finally(() => {
            if (active) setRefreshing(false);
        });
        return () => {
            active = false;
        };
    }, [refreshCurrentUser]);

    if (refreshing) {
        return <LoadingScreen/>;
    }

    const roles = currentUser?.roles;

    if (roles?.has(UserxRole.MANAGEMENT)) {
        return <Navigate to={ROUTES.MANAGEMENT_DASHBOARD} replace/>;
    }

    if (roles?.has(UserxRole.DEPARTMENT_LEAD)) {
        return <Navigate to={ROUTES.DEPARTMENT_DASHBOARD} replace/>;
    }

    if (roles?.has(UserxRole.EMPLOYEE)) {
        return <Navigate to={ROUTES.DASHBOARD} replace/>;
    }

    if (roles?.has(UserxRole.BUILDING_ADMIN)) {
        return <Navigate to={ROUTES.ROOMS} replace/>;
    }

    if (roles?.has(UserxRole.SYSTEM_ADMIN)) {
        return <Navigate to={ROUTES.MANAGE_USERS} replace/>;
    }

    return <Navigate to={ROUTES.PROFILE} replace/>;
};

export default HomePage;

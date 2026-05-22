import {Navigate} from "react-router-dom";
import {useUser} from "../Contexts/AuthenticatedUserContext";
import {UserxRole} from "../generated-skeleton-api";
import {ROUTES} from "../utilities/routes.paths";

/**
 * The logo/home route should not be a placeholder page. Send users to the
 * most relevant implemented view for their role.
 */
const HomePage = () => {
    const {currentUser} = useUser();
    const roles = currentUser?.roles;

    if (roles?.has(UserxRole.MANAGEMENT)) {
        return <Navigate to={ROUTES.MANAGEMENT_DASHBOARD} replace/>;
    }

    if (roles?.has(UserxRole.DEPARTMENT_LEAD) || roles?.has(UserxRole.EMPLOYEE)) {
        return <Navigate to={ROUTES.DASHBOARD} replace/>;
    }

    if (roles?.has(UserxRole.BUILDING_ADMIN)) {
        return <Navigate to={ROUTES.DEVICES} replace/>;
    }

    if (roles?.has(UserxRole.SYSTEM_ADMIN)) {
        return <Navigate to={ROUTES.MANAGE_USERS} replace/>;
    }

    return <Navigate to={ROUTES.PROFILE} replace/>;
};

export default HomePage;

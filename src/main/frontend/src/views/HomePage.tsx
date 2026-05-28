import {Navigate} from "react-router-dom";
import {ROUTES} from "../utilities/routes.paths";

/**
 * The logo/home route should stay cheap. Send all authenticated users to the
 * shared profile view instead of role-specific dashboards that may load
 * large analytics data sets.
 */
const HomePage = () => {
    return <Navigate to={ROUTES.PROFILE} replace/>;
};

export default HomePage;

/**
 * This code is part of the skeleton project provided for students of the course "Software
 * Engineering" offered by Innsbruck University.
 */
import './styles/App.css';
import "primereact/resources/themes/lara-light-cyan/theme.css";
import React, {Suspense} from "react";
import {BrowserRouter, Route, Routes} from "react-router-dom";
import {HomePageRoute, LoginsRoute, LogoutsRoute, ManageUsersRoute} from "./routes";
import PrivateRoute from './components/PrivateRoute';
import {UserProvider} from "./Contexts/AuthenticatedUserContext";
import {UserxRole} from "./generated-skeleton-api";
import {ROUTES} from "./utilities/routes.paths";

const EmployeeDashboard = React.lazy(() => import('./views/EmployeeDashboard'));
const RoomHistory = React.lazy(() => import('./views/RoomHistory'));
const AbsenceView = React.lazy(() => import('./views/AbsenceView'));

const App: React.FC = () => {
    return (
        <UserProvider>
            <Suspense fallback={<div>Loading...</div>}>
                <BrowserRouter>
                    <Routes>
                        {/* Public route */}
                        <Route path={LoginsRoute.url} Component={LoginsRoute.component}/>

                        {/* Routes accessible to all authenticated users */}
                        <Route element={<PrivateRoute/>}>
                            <Route path={HomePageRoute.url} Component={HomePageRoute.component}/>
                            <Route path={LogoutsRoute.url} Component={LogoutsRoute.component}/>
                            <Route path={ROUTES.DASHBOARD} Component={EmployeeDashboard}/>
                            <Route path={ROUTES.DASHBOARD_HISTORY} Component={RoomHistory}/>
                        </Route>

                        {/* Employee and Department Lead */}
                        <Route element={<PrivateRoute roles={[UserxRole.EMPLOYEE, UserxRole.DEPARTMENT_LEAD]}/>}>
                            <Route path={ROUTES.ABSENCE} Component={AbsenceView}/>
                        </Route>

                        {/* Building Admin and System Admin only */}
                        <Route element={<PrivateRoute roles={[UserxRole.BUILDING_ADMIN, UserxRole.SYSTEM_ADMIN]}/>}>
                            <Route path={ManageUsersRoute.url} Component={ManageUsersRoute.component}/>
                        </Route>
                    </Routes>
                </BrowserRouter>
            </Suspense>
        </UserProvider>
    );
}

export default App;

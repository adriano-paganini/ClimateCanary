/**
 * This code is part of the skeleton project provided for students of the course "Software
 * Engineering" offered by Innsbruck University.
 */
import './styles/App.css';
import "primereact/resources/themes/lara-light-cyan/theme.css";
import React, {Suspense} from "react";
import {BrowserRouter, Outlet, Route, Routes} from "react-router-dom";
import {HomePageRoute, LoginsRoute, LogoutsRoute, ManageUsersRoute} from "./routes";
import PrivateRoute from './components/PrivateRoute';
import {UserProvider} from "./Contexts/AuthenticatedUserContext";
import {DepartmentProvider} from "./Contexts/DepartmentContext";
import {UserxRole} from "./generated-skeleton-api";
import {ROUTES} from "./utilities/routes.paths";
import LoadingScreen from "./components/LoadingScreen";

const EmployeeDashboard = React.lazy(() => import('./views/EmployeeDashboard'));
const RoomHistory = React.lazy(() => import('./views/RoomHistory'));
const AbsenceView = React.lazy(() => import('./views/AbsenceView'));
const UserProfileView = React.lazy(() => import('./views/UserProfileView'));
const DepartmentDashboard = React.lazy(() => import('./views/DepartmentDashboard'));
const DepartmentAbsenceView = React.lazy(() => import('./views/DepartmentAbsenceView'));
const ManagementDashboard = React.lazy(() => import('./views/ManagementDashboard'));
const DeviceManagementView = React.lazy(() => import('./views/DeviceManagementView'));
const OrgStructureView = React.lazy(() => import('./views/OrgStructureView'));
const ThresholdManagementView = React.lazy(() => import('./views/ThresholdManagementView'));
const RoomManagementView = React.lazy(() => import('./views/RoomManagementView'));

const App: React.FC = () => {
    return (
        <UserProvider>
            <Suspense fallback={<LoadingScreen/>}>
                <BrowserRouter>
                    <Routes>
                        {/* Public route */}
                        <Route path={LoginsRoute.url} Component={LoginsRoute.component}/>

                        {/* Routes accessible to all authenticated users */}
                        <Route element={<PrivateRoute/>}>
                            <Route path={HomePageRoute.url} Component={HomePageRoute.component}/>
                            <Route path={LogoutsRoute.url} Component={LogoutsRoute.component}/>
                            <Route path={ROUTES.PROFILE} Component={UserProfileView}/>
                        </Route>

                        {/* Employee only */}
                        <Route element={<PrivateRoute roles={[UserxRole.EMPLOYEE]}/>}>
                            <Route path={ROUTES.DASHBOARD} Component={EmployeeDashboard}/>
                            <Route path={ROUTES.DASHBOARD_HISTORY} Component={RoomHistory}/>
                            <Route path={ROUTES.ABSENCE} Component={AbsenceView}/>
                        </Route>

                        {/* Department Lead only */}
                        <Route element={<PrivateRoute roles={[UserxRole.DEPARTMENT_LEAD]}/>}>
                            <Route element={<DepartmentProvider><Outlet/></DepartmentProvider>}>
                                <Route path={ROUTES.DEPARTMENT_DASHBOARD} Component={DepartmentDashboard}/>
                                <Route path={ROUTES.DEPARTMENT_ROOM_HISTORY} Component={RoomHistory}/>
                                <Route path={ROUTES.DEPARTMENT_ABSENCES} Component={DepartmentAbsenceView}/>
                            </Route>
                        </Route>

                        {/* Management only */}
                        <Route element={<PrivateRoute roles={[UserxRole.MANAGEMENT]}/>}>
                            <Route path={ROUTES.MANAGEMENT_DASHBOARD} Component={ManagementDashboard}/>
                            <Route path={ROUTES.MANAGEMENT_ROOM_HISTORY} Component={RoomHistory}/>
                        </Route>

                        {/* Building Admin only */}
                        <Route element={<PrivateRoute roles={[UserxRole.BUILDING_ADMIN]}/>}>
                            <Route path={ROUTES.ROOMS} Component={RoomManagementView}/>
                            <Route path={ROUTES.THRESHOLDS} Component={ThresholdManagementView}/>
                        </Route>

                        {/* System Admin only */}
                        <Route element={<PrivateRoute roles={[UserxRole.SYSTEM_ADMIN]}/>}>
                            <Route path={ROUTES.BUILDINGS} Component={OrgStructureView}/>
                            <Route path={ManageUsersRoute.url} Component={ManageUsersRoute.component}/>
                            <Route path={ROUTES.DEVICES} Component={DeviceManagementView}/>
                        </Route>
                    </Routes>
                </BrowserRouter>
            </Suspense>
        </UserProvider>
    );
}

export default App;

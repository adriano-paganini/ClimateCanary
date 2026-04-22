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

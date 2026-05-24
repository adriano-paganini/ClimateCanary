/**
 * This code is part of the skeleton project provided for students of the course "Software
 * Engineering" offered by Innsbruck University.
 */
import React from 'react';
import {Menubar} from "primereact/menubar";

import {useUser} from "../Contexts/AuthenticatedUserContext";
import {menuConfig, MenuItemConfig} from "../config/menuConfig";
import {UserxRole} from "../generated-skeleton-api";
import {MenuItem} from "primereact/menuitem";
import {Link, NavLink, useLocation} from "react-router-dom";
import {ROUTES} from "../utilities/routes.paths";
import "../styles/Navbar.css";

/**
 * Navbar component.
 */
const NavbarComponent: React.FC = () => {
    const { currentUser: user } = useUser();
    const { pathname } = useLocation();

    const filterMenu = React.useCallback((items: MenuItemConfig[]): MenuItemConfig[] => {
        if (!user) return [];

        const hasRole = (required?: UserxRole[]) =>
            !required || required.some(r => {
                return user.roles?.has(r) ?? false;
            });

        return items
            .map(item => {
                const visibleChildren = item.items ? filterMenu(item.items) : undefined;
                return {...item, items: visibleChildren};
            })
            .filter(item => {
                const visible = hasRole(item.roles);
                const hasChildren = !!item.items?.length;
                // Keep if user can see it, or it has visible children
                return visible || hasChildren;
            });
    }, [user]);

    // we want to use navigate (react router) to ensure pure client-side navigation on menu item click
    // incidentally, we also want to fix primereact component-related aria warnings
    const isRouteActive = React.useCallback((route?: string): boolean => {
        if (!route) return false;
        if (route === ROUTES.HOME) return pathname === route;
        if (route === ROUTES.DEPARTMENT_DASHBOARD) return pathname === route;

        return pathname === route || pathname.startsWith(`${route}/`);
    }, [pathname]);

    const hasActiveChild = React.useCallback((item: MenuItemConfig): boolean => {
        return isRouteActive(item.route) || !!item.items?.some(child => hasActiveChild(child));
    }, [isRouteActive]);

    const buildMenubar = React.useCallback((items: MenuItemConfig[]): MenuItem[] => {
        return items.map(configItem => {
            const children = configItem.items ? buildMenubar(configItem.items) : undefined;
            const childActive = hasActiveChild(configItem);

            const menuItem: MenuItem = {
                label: configItem.label,
                icon: configItem.icon,
                items: children,
            };

            menuItem.template = (menuItem, options) => {
                const base = options.className ?? "";

                // Logo
                if (configItem.route === ROUTES.HOME) {
                    return (
                        <Link
                            to={ROUTES.HOME}
                            className={`${base} p-menuitem-link navbar-home-link`}
                            title="ClimateCanary"
                        >
                            <img src="/32x32.png" alt="ClimateCanary" className="navbar-home-logo"/>
                        </Link>
                    );
                }

                // Group item (has children, no route) — opens submenu, no navigation
                if (!configItem.route) {
                    const childCount = children?.length ?? 0;

                    return (
                        <a
                            href="#"
                            className={`${base} p-menuitem-link navbar-group-link${childActive ? " navbar-group-link--active" : ""}`}
                            onClick={(e) => { e.preventDefault(); options.onClick?.(e); }}
                            aria-current={childActive ? "page" : undefined}
                        >
                            {menuItem.icon && <span className={options.iconClassName}/>}
                            <span className={options.labelClassName}>{menuItem.label}</span>
                            <span className="navbar-group-meta" aria-hidden="true">
                                {childCount > 0 && <span className="navbar-group-count">{childCount}</span>}
                                <i className="pi pi-chevron-right navbar-group-chevron"/>
                            </span>
                            <span className="navbar-sr-only">Toggle {menuItem.label} menu</span>
                        </a>
                    );
                }

                // Regular nav item — highlight when active
                return (
                    <NavLink
                        to={configItem.route}
                        end={configItem.route === ROUTES.HOME || configItem.route === ROUTES.DEPARTMENT_DASHBOARD}
                        className={({ isActive }) =>
                            `${base} p-menuitem-link${isActive ? " navbar-link--active" : ""}`
                        }
                        onClick={(e) => options.onClick?.(e)}
                    >
                        {menuItem.icon && <span className={options.iconClassName}/>}
                        <span className={options.labelClassName}>{menuItem.label}</span>
                    </NavLink>
                );
            }
            return menuItem;
        });
    }, [hasActiveChild]);

    const filteredItems = React.useMemo(() => filterMenu(menuConfig), [filterMenu]);

    const model = React.useMemo(() => buildMenubar(filteredItems), [filteredItems, buildMenubar]);

    // don't render Menubar if no user is logged in
    if (!user) {
        return null;
    }

    const displayUser = user;
    const displayName = [displayUser.firstName, displayUser.lastName].filter(Boolean).join(' ') || displayUser.username;
    const roles = displayUser.roles ? [...displayUser.roles] : [];

    const profileEnd = (
        <div className="navbar-user-actions">
            <Link to={ROUTES.LOGOUT} title="Logout" className="navbar-logout">
                <i className="pi pi-sign-out" aria-hidden="true"/>
                <span>Logout</span>
            </Link>
            <Link to={ROUTES.PROFILE} title="My Profile" className="navbar-profile">
                <div className="navbar-profile__details">
                    <span className="navbar-profile__username">@{displayUser.username}</span>
                    <span className="navbar-profile__name">{displayName}</span>
                </div>
                <div className="navbar-profile__roles" aria-label="User roles">
                    <span className="navbar-profile__roles-title">Roles</span>
                    {roles.length > 0 ? roles.map(role => (
                        <span key={role} className="navbar-profile__role">{role}</span>
                    )) : (
                        <span className="navbar-profile__role navbar-profile__role--empty">No roles</span>
                    )}
                </div>
            </Link>
        </div>
    );

    return (
        <div className="card">
            <Menubar model={model} end={profileEnd}/>
        </div>
    );
}

export default NavbarComponent;

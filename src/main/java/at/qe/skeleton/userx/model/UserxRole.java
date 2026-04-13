package at.qe.skeleton.userx.model;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.security.core.GrantedAuthority;

/**
 * Enumeration of available user roles.
 *
 * This class is part of the skeleton project provided for students of the
 * course "Software Engineering" offered by the University of Innsbruck.
*/
@Schema(description = "Enumeration of available user roles.", enumAsRef = true)
public enum UserxRole implements GrantedAuthority {

    SYSTEM_ADMIN,
    BUILDING_ADMIN,
    DEPARTMENT_LEAD,
    MANAGEMENT,
    EMPLOYEE;

    @Override
    public String getAuthority() {
        return name();
    }
}

package at.qe.skeleton.tests.services;

import at.qe.skeleton.models.Userx;
import at.qe.skeleton.models.EmployeeProfile;
import at.qe.skeleton.models.Room;
import at.qe.skeleton.models.Department;
import at.qe.skeleton.models.UserxRole;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests to ensure that each entity's implementation of equals conforms to the
 * contract. See <a href="http://www.jqno.nl/equalsverifier/">EqualsVerifier</a>
 * for more information.
 *
 * <p>This class is part of the skeleton project provided for students of the
 * course "Software Engineering" offered by the University of Innsbruck.
 */
public class EqualsImplementationTest {

    @Test
    public void testUserEqualsContract() {
        Userx user1 = new Userx();
        ReflectionTestUtils.setField(user1, "id", 1L);
        Userx user2 = new Userx();
        ReflectionTestUtils.setField(user2, "id", 2L);
        EmployeeProfile profile1 = new EmployeeProfile();
        ReflectionTestUtils.setField(profile1, "id", 1L);
        EmployeeProfile profile2 = new EmployeeProfile();
        ReflectionTestUtils.setField(profile2, "id", 2L);
        Room room1 = new Room();
        ReflectionTestUtils.setField(room1, "id", 1L);
        Room room2 = new Room();
        ReflectionTestUtils.setField(room2, "id", 2L);
        Department department1 = new Department();
        ReflectionTestUtils.setField(department1, "id", 1L);
        Department department2 = new Department();
        ReflectionTestUtils.setField(department2, "id", 2L);

        EqualsVerifier.forClass(Userx.class)
                .withPrefabValues(Userx.class, user1, user2)
                .withPrefabValues(EmployeeProfile.class, profile1, profile2)
                .withPrefabValues(Room.class, room1, room2)
                .withPrefabValues(Department.class, department1, department2)
                .suppress(Warning.STRICT_INHERITANCE, Warning.ALL_FIELDS_SHOULD_BE_USED)
                .verify();
    }

    @Test
    public void testUserRoleEqualsContract() {
        EqualsVerifier.forClass(UserxRole.class).verify();
    }

}

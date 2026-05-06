package at.qe.skeleton.tests.services;

import at.qe.skeleton.common.exceptions.NotFoundException;
import at.qe.skeleton.dtos.UserxSelfUpdateDTO;
import at.qe.skeleton.dtos.UserxUpdateDTO;
import at.qe.skeleton.models.Department;
import at.qe.skeleton.models.EmployeeProfile;
import at.qe.skeleton.models.Userx;
import at.qe.skeleton.models.UserxRole;
import at.qe.skeleton.repositories.DepartmentRepository;
import at.qe.skeleton.services.UserxService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.collections.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Some very basic tests for {@link UserxService}.
 *
 * This class is part of the skeleton project provided for students of the courses "Software
 * Engineering" offered by the University of Innsbruck.
 */


@SpringBootTest()
@WebAppConfiguration
public class UserxServiceTest {

    @Autowired
    UserxService userService;

    @Autowired
    DepartmentRepository departmentRepository;

    @Test
    @WithMockUser(username = "admin", authorities = {"SYSTEM_ADMIN"})
    public void testDatainitialization() {
        Assertions.assertEquals(4, userService.getAllUsers().size(),
                "Insufficient amount of users initialized for test data source");
        for (Userx user : userService.getAllUsers()) {
            switch (user.getUsername()) {
                case "admin" -> {
                    Assertions.assertTrue(user.getRoles().contains(UserxRole.SYSTEM_ADMIN),
                            "User \"" + user + "\" does not have role ADMIN");
                    Assertions.assertNotNull(user.getCreateUser(),
                            "User \"" + user + "\" does not have a createUser defined");
                    Assertions.assertNotNull(user.getCreateDate(),
                            "User \"" + user + "\" does not have a createDate defined");
                    Assertions.assertNull(user.getUpdateUser(),
                            "User \"" + user + "\" has a updateUser defined");
                    Assertions.assertNull(user.getUpdateDate(),
                            "User \"" + user + "\" has a updateDate defined");
                }
                case "user1" -> {
                    Assertions.assertTrue(user.getRoles().contains(UserxRole.MANAGEMENT),
                            "User \"" + user + "\" does not have role MANAGER");
                    Assertions.assertNotNull(user.getCreateUser(),
                            "User \"" + user + "\" does not have a createUser defined");
                    Assertions.assertNotNull(user.getCreateDate(),
                            "User \"" + user + "\" does not have a createDate defined");
                    Assertions.assertNull(user.getUpdateUser(),
                            "User \"" + user + "\" has a updateUser defined");
                    Assertions.assertNull(user.getUpdateDate(),
                            "User \"" + user + "\" has a updateDate defined");
                }
                case "user2" -> {
                    Assertions.assertTrue(user.getRoles().contains(UserxRole.EMPLOYEE),
                            "User \"" + user + "\" does not have role EMPLOYEE");
                    Assertions.assertNotNull(user.getCreateUser(),
                            "User \"" + user + "\" does not have a createUser defined");
                    Assertions.assertNotNull(user.getCreateDate(),
                            "User \"" + user + "\" does not have a createDate defined");
                    Assertions.assertNull(user.getUpdateUser(),
                            "User \"" + user + "\" has a updateUser defined");
                    Assertions.assertNull(user.getUpdateDate(),
                            "User \"" + user + "\" has a updateDate defined");
                }
                case "elvis" -> {
                    Assertions.assertTrue(user.getRoles().contains(UserxRole.SYSTEM_ADMIN),
                            "User \"" + user + "\" does not have role ADMIN");
                    Assertions.assertNotNull(user.getCreateUser(),
                            "User \"" + user + "\" does not have a createUser defined");
                    Assertions.assertNotNull(user.getCreateDate(),
                            "User \"" + user + "\" does not have a createDate defined");
                    Assertions.assertNull(user.getUpdateUser(),
                            "User \"" + user + "\" has a updateUser defined");
                    Assertions.assertNull(user.getUpdateDate(),
                            "User \"" + user + "\" has a updateDate defined");
                }
                case null, default -> Assertions.fail(
                        "Unknown user \"" + user.getUsername() + "\" loaded from test data source via UserService.getAllUsers");
            }
        }
    }

    @DirtiesContext
    @Test
    @WithMockUser(username = "admin", authorities = {"SYSTEM_ADMIN"})
    public void testDeleteUser() {
        Long deleteUserId = 2000L;
        Optional<Userx> adminUser = userService.loadUser(1000L);
        Assertions.assertFalse(adminUser.isEmpty(),
                "Admin user could not be loaded from test data source");
        Optional<Userx> toBeDeletedUserOpt = userService.loadUser(deleteUserId);
        Assertions.assertFalse(toBeDeletedUserOpt.isEmpty(),
                "User with id \"" + deleteUserId + "\" could not be loaded from test data source");
        Userx toBeDeletedUser = toBeDeletedUserOpt.get();

        userService.deleteUser(toBeDeletedUser);

        Assertions.assertEquals(3, userService.getAllUsers().size(),
                "No user has been enabled after calling UserService.deleteUser");
        Optional<Userx> deletedUserOpt = userService.loadUser(deleteUserId);
        Assertions.assertTrue(deletedUserOpt.isEmpty(),
                "Deleted User with id \"" + deleteUserId + "\" could still be loaded from test data source via UserService.loadUser");

        for (Userx remainingUser : userService.getAllUsers()) {
            Assertions.assertNotEquals(toBeDeletedUser.getUsername(), remainingUser.getUsername(),
                    "Deleted User with id \"" + deleteUserId + "\" could still be loaded from test data source via UserService.getAllUsers");
        }
    }

    @DirtiesContext
    @Test
    @WithMockUser(username = "admin", authorities = {"SYSTEM_ADMIN"})
    public void testUpdateUser() {
        Long userId = 2000L;
        Optional<Userx> adminUserOpt = userService.loadUser(1000L);
        Assertions.assertNotNull(adminUserOpt, "Admin user could not be loaded from test data source");
        Userx adminUser = adminUserOpt.get();

        Optional<Userx> toBeSavedUserOpt = userService.loadUser(userId);
        Assertions.assertFalse(toBeSavedUserOpt.isEmpty(),
                "User with id \"" + userId + "\" could not be loaded from test data source");
        Userx toBeSavedUser = toBeSavedUserOpt.get();

        Assertions.assertNull(toBeSavedUser.getUpdateUser(),
                "User with id \"" + userId + "\" has a updateUser defined");
        Assertions.assertNull(toBeSavedUser.getUpdateDate(),
                "User with id \"" + userId + "\" has a updateDate defined");

        toBeSavedUser.setEmail("changed-email@whatever.wherever");
        userService.saveUser(toBeSavedUser);

        Optional<Userx> freshlyLoadedUserOpt = userService.loadUser(userId);
        Assertions.assertFalse(freshlyLoadedUserOpt.isEmpty(),
                "User with id \"" + userId + "\" could not be loaded from test data source after being saved");
        Userx freshlyLoadedUser = freshlyLoadedUserOpt.get();
        Assertions.assertNotNull(freshlyLoadedUser.getUpdateUser(),
                "User with id \"" + userId + "\" does not have a updateUser defined after being saved");
        Assertions.assertEquals(adminUser, freshlyLoadedUser.getUpdateUser(),
                "User with id \"" + userId + "\" has wrong updateUser set");
        Assertions.assertNotNull(freshlyLoadedUser.getUpdateDate(),
                "User with id \"" + userId + "\" does not have a updateDate defined after being saved");
        Assertions.assertEquals("changed-email@whatever.wherever", freshlyLoadedUser.getEmail(),
                "User with id \"" + userId + "\" does not have a the correct email attribute stored being saved");
    }

    @DirtiesContext
    @Test
    @WithMockUser(username = "admin", authorities = {"SYSTEM_ADMIN"})
    public void testCreateUser() {
        Optional<Userx> adminUserOpt = userService.loadUser(1000L);
        Assertions.assertFalse(adminUserOpt.isEmpty(),
                "Admin user could not be loaded from test data source");
        Userx adminUser = adminUserOpt.get();

        String username = "newuser";
        String password = "passwd";
        String fName = "New";
        String lName = "User";
        String email = "new-email@whatever.wherever";
        String phone = "+12 345 67890";
        Userx toBeCreatedUser = new Userx();
        toBeCreatedUser.setUsername(username);
        toBeCreatedUser.setPassword(password);
        toBeCreatedUser.setEnabled(true);
        toBeCreatedUser.setFirstName(fName);
        toBeCreatedUser.setLastName(lName);
        toBeCreatedUser.setEmail(email);
        toBeCreatedUser.setPhone(phone);
        toBeCreatedUser.setRoles(Sets.newSet(UserxRole.EMPLOYEE, UserxRole.MANAGEMENT));
        Userx savedUser = userService.saveUser(toBeCreatedUser);

        Optional<Userx> freshlyCreatedUserOpt = userService.loadUser(savedUser.getId());
        Assertions.assertFalse(freshlyCreatedUserOpt.isEmpty(),
                "New user could not be loaded from test data source after being saved");
        Userx freshlyCreatedUser = freshlyCreatedUserOpt.get();

        Assertions.assertEquals(username, freshlyCreatedUser.getUsername(),
                "New user could not be loaded from test data source after being saved");
        Assertions.assertTrue(
                BCrypt.checkpw(password, freshlyCreatedUser.getPassword().replace("{bcrypt}", "")),
                "User \"" + username + "\" password does not match the original password");
        Assertions.assertEquals(fName, freshlyCreatedUser.getFirstName(),
                "User \"" + username + "\" does not have a the correct firstName attribute stored being saved");
        Assertions.assertEquals(lName, freshlyCreatedUser.getLastName(),
                "User \"" + username + "\" does not have a the correct lastName attribute stored being saved");
        Assertions.assertEquals(email, freshlyCreatedUser.getEmail(),
                "User \"" + username + "\" does not have a the correct email attribute stored being saved");
        Assertions.assertEquals(phone, freshlyCreatedUser.getPhone(),
                "User \"" + username + "\" does not have a the correct phone attribute stored being saved");
        Assertions.assertTrue(freshlyCreatedUser.getRoles().contains(UserxRole.MANAGEMENT),
                "User \"" + username + "\" does not have role MANAGER");
        Assertions.assertTrue(freshlyCreatedUser.getRoles().contains(UserxRole.EMPLOYEE),
                "User \"" + username + "\" does not have role EMPLOYEE");
        Assertions.assertNotNull(freshlyCreatedUser.getCreateUser(),
                "User \"" + username + "\" does not have a createUser defined after being saved");
        Assertions.assertEquals(adminUser, freshlyCreatedUser.getCreateUser(),
                "User \"" + username + "\" has wrong createUser set");
        Assertions.assertNotNull(freshlyCreatedUser.getCreateDate(),
                "User \"" + username + "\" does not have a createDate defined after being saved");
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"SYSTEM_ADMIN"})
    public void testExceptionForEmptyUsername() {
        Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> {
            Optional<Userx> adminUser = userService.loadUser(1000L);
            Assertions.assertFalse(adminUser.isEmpty(),
                    "Admin user could not be loaded from test data source");

            Userx toBeCreatedUser = new Userx();
            toBeCreatedUser.setPassword("passwd");
            userService.saveUser(toBeCreatedUser);
        });
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"SYSTEM_ADMIN"})
    public void testExceptionForEmptyUser() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Optional<Userx> adminUser = userService.loadUser(1000L);
            Assertions.assertFalse(adminUser.isEmpty(),
                    "Admin user could not be loaded from test data source");

            Userx toBeCreatedUser = new Userx();
            userService.saveUser(toBeCreatedUser);
        });
    }

    @Test
    public void testUnauthenticatedLoadUsers() {
        Assertions.assertThrows(
                org.springframework.security.authentication.AuthenticationCredentialsNotFoundException.class,
                () -> {
                    for (Userx user : userService.getAllUsers()) {
                        Assertions.fail(
                                "Call to userService.getAllUsers should not work without proper authorization");
                    }
                });
    }

    @Test
    @WithMockUser(username = "user", authorities = {"EMPLOYEE"})
    public void testUnauthorizedLoadUsers() {
        Assertions.assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            for (Userx user : userService.getAllUsers()) {
                Assertions.fail(
                        "Call to userService.getAllUsers should not work without proper authorization");
            }
        });
    }

    @Test
    @WithMockUser(username = "user1", authorities = {"EMPLOYEE"})
    public void testUnauthorizedLoadUser() {
        Assertions.assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            Optional<Userx> user = userService.loadUser(1000L);
            Assertions.fail(
                    "Call to userService.loadUser should not work without proper authorization for other users than the authenticated one");
        });
    }

    @Test
    @WithMockUser(username = "user1", authorities = {"EMPLOYEE"})
    public void testUnauthorizedSaveUser() {
        Assertions.assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            Long userId = 2000L;
            Optional<Userx> userOpt = userService.loadUser(userId);
            Assertions.assertFalse(userOpt.isEmpty());
            Userx user = userOpt.get();

            Assertions.assertEquals(userId, user.getId(),
                    "Call to userService.loadUser returned wrong user");
            userService.saveUser(user);
        });
    }

    @Test
    @WithMockUser(username = "user1", authorities = {"EMPLOYEE"})
    public void testUnauthorizedDeleteUser() {
        Assertions.assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            Long userId = 2000L;
            Optional<Userx> userOpt = userService.loadUser(userId);
            Assertions.assertFalse(userOpt.isEmpty());
            Userx user = userOpt.get();

            Assertions.assertEquals(userId, user.getId(),
                    "Call to userService.loadUser returned wrong user");
            userService.deleteUser(user);
        });
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"SYSTEM_ADMIN"})
    public void testGetUserByIdFound() {
        Userx user = userService.getUserById(2000L);
        Assertions.assertNotNull(user,
                "getUserById should return a user for a valid id");
        Assertions.assertEquals(2000L, user.getId(),
                "getUserById returned a user with the wrong id");
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"SYSTEM_ADMIN"})
    public void testGetUserByIdNotFound() {
        Assertions.assertThrows(NotFoundException.class,
                () -> userService.getUserById(9999L),
                "getUserById should throw NotFoundException for an unknown id");
    }

    @Test
    public void testGetUserByUsernameFound() {
        Userx user = userService.getUserByUsername("user1");
        Assertions.assertNotNull(user,
                "getUserByUsername should return a user for a known username");
        Assertions.assertEquals("user1", user.getUsername(),
                "getUserByUsername returned a user with the wrong username");
    }

    @Test
    public void testGetUserByUsernameNotFound() {
        Userx user = userService.getUserByUsername("doesnotexist");
        Assertions.assertNull(user,
                "getUserByUsername should return null for an unknown username");
    }

    @DirtiesContext
    @Test
    @WithMockUser(username = "user1", authorities = {"EMPLOYEE"})
    public void testSaveCurrentUserUpdatesFields() {
        UserxSelfUpdateDTO dto = new UserxSelfUpdateDTO(
                "UpdatedFirst", "UpdatedLast", "updated@example.com", "+43 512 000000"
        );

        Userx updated = userService.saveCurrentUser(dto);

        Assertions.assertEquals("UpdatedFirst", updated.getFirstName(),
                "saveCurrentUser did not update firstName");
        Assertions.assertEquals("UpdatedLast", updated.getLastName(),
                "saveCurrentUser did not update lastName");
        Assertions.assertEquals("updated@example.com", updated.getEmail(),
                "saveCurrentUser did not update email");
        Assertions.assertEquals("+43 512 000000", updated.getPhone(),
                "saveCurrentUser did not update phone");
        Assertions.assertNotNull(updated.getUpdateUser(),
                "saveCurrentUser did not set updateUser");
        Assertions.assertNotNull(updated.getUpdateDate(),
                "saveCurrentUser did not set updateDate");
    }

    @DirtiesContext
    @Test
    @WithMockUser(username = "user1", authorities = {"EMPLOYEE"})
    public void testSaveCurrentUserSkipsNullFields() {
        // Load the original values so we can assert they were not overwritten
        Userx original = userService.getUserByUsername("user1");
        String originalFirstName = original.getFirstName();
        String originalEmail    = original.getEmail();

        // Only update phone; leave all other fields null
        UserxSelfUpdateDTO dto = new UserxSelfUpdateDTO(null, null, null, "+43 512 999999");

        Userx updated = userService.saveCurrentUser(dto);

        Assertions.assertEquals(originalFirstName, updated.getFirstName(),
                "saveCurrentUser must not overwrite firstName when DTO field is null");
        Assertions.assertEquals(originalEmail, updated.getEmail(),
                "saveCurrentUser must not overwrite email when DTO field is null");
        Assertions.assertEquals("+43 512 999999", updated.getPhone(),
                "saveCurrentUser did not update the phone field");
    }

    @DirtiesContext
    @Test
    @WithMockUser(username = "admin", authorities = {"SYSTEM_ADMIN"})
    public void testAdminUpdateUserFields() {
        Long userId = 3000L;
        UserxUpdateDTO dto = new UserxUpdateDTO(
                "admin-updated@example.com",
                new HashSet<>(Set.of(UserxRole.MANAGEMENT)),
                "AdminFirst",
                "AdminLast",
                "+1 000 0000",
                null
        );

        Userx updated = userService.updateUser(userId, dto);

        Assertions.assertEquals("AdminFirst", updated.getFirstName(),
                "updateUser did not update firstName");
        Assertions.assertEquals("AdminLast", updated.getLastName(),
                "updateUser did not update lastName");
        Assertions.assertEquals("admin-updated@example.com", updated.getEmail(),
                "updateUser did not update email");
        Assertions.assertEquals("+1 000 0000", updated.getPhone(),
                "updateUser did not update phone");
        Assertions.assertTrue(updated.getRoles().contains(UserxRole.MANAGEMENT),
                "updateUser did not update roles");
        Assertions.assertNotNull(updated.getUpdateUser(),
                "updateUser did not set updateUser");
        Assertions.assertNotNull(updated.getUpdateDate(),
                "updateUser did not set updateDate");
    }

    @DirtiesContext
    @Test
    @WithMockUser(username = "admin", authorities = {"SYSTEM_ADMIN"})
    public void testAdminUpdateUserEnablesUser() {
        Long userId = 3000L;

        // Disable first so there is a meaningful state transition to test
        userService.updateUser(userId, new UserxUpdateDTO(null, null, null, null, null, false));

        UserxUpdateDTO enableDto = new UserxUpdateDTO(null, null, null, null, null, true);
        Userx updated = userService.updateUser(userId, enableDto);

        Assertions.assertTrue(updated.isEnabled(),
                "updateUser with enabled=true should enable the user");
    }

    @DirtiesContext
    @Test
    @WithMockUser(username = "admin", authorities = {"SYSTEM_ADMIN"})
    public void testAdminUpdateUserDisablesClearsDepartmentLeader() {
        Long userId = 3000L;
        Userx user = userService.getUserById(userId);

        Department dept = new Department();
        dept.setName("Test Department");
        dept.setDepartmentLeader(user);
        dept = departmentRepository.save(dept);
        Long deptId = dept.getId();

        UserxUpdateDTO dto = new UserxUpdateDTO(null, null, null, null, null, false);
        Userx updated = userService.updateUser(userId, dto);

        Assertions.assertFalse(updated.isEnabled(),
                "updateUser with enabled=false should disable the user");

        Department reloaded = departmentRepository.findById(deptId).orElseThrow();
        Assertions.assertNull(reloaded.getDepartmentLeader(),
                "Disabling a user should clear them as department leader");
    }

    @DirtiesContext
    @Test
    @WithMockUser(username = "admin", authorities = {"SYSTEM_ADMIN"})
    public void testAdminUpdateUserDisablesClearsEmployeeProfileAndAbsences() {
        Long userId = 3000L;
        Userx user = userService.getUserById(userId);

        // Attach an employee profile directly via the user entity and re-save
        EmployeeProfile profile = new EmployeeProfile();
        profile.setUser(user);
        user.setEmployeeProfile(profile);
        userService.saveUser(user);

        UserxUpdateDTO dto = new UserxUpdateDTO(null, null, null, null, null, false);
        Userx updated = userService.updateUser(userId, dto);

        Assertions.assertNull(updated.getEmployeeProfile(),
                "Disabling a user should clear their employee profile");
        Assertions.assertTrue(updated.getAbsences().isEmpty(),
                "Disabling a user should clear their absences");
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"SYSTEM_ADMIN"})
    public void testAdminUpdateUserNotFoundThrows() {
        UserxUpdateDTO dto = new UserxUpdateDTO(null, null, null, null, null, null);

        Assertions.assertThrows(NotFoundException.class,
                () -> userService.updateUser(9999L, dto),
                "updateUser should throw NotFoundException for an unknown id");
    }

    @DirtiesContext
    @Test
    @WithMockUser(username = "user2", authorities = {"EMPLOYEE"})
    public void testDeleteCurrentUser() {
        Userx before = userService.getUserByUsername("user2");
        Assertions.assertNotNull(before, "user2 should exist before deletion");

        userService.deleteCurrentUser();

        Userx after = userService.getUserByUsername("user2");
        Assertions.assertNull(after,
                "user2 should no longer exist after deleteCurrentUser");
    }
}

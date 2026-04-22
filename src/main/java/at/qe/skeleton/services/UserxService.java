package at.qe.skeleton.services;

import at.qe.skeleton.common.exceptions.UserNotFoundException;
import at.qe.skeleton.common.exceptions.UsernameDuplicateException;
import at.qe.skeleton.models.Department;
import at.qe.skeleton.repositories.DepartmentRepository;
import at.qe.skeleton.repositories.EmployeeProfileRepository;
import at.qe.skeleton.dtos.UserxSelfUpdateDTO;
import at.qe.skeleton.dtos.UserxUpdateDTO;
import at.qe.skeleton.models.Userx;
import at.qe.skeleton.repositories.UserxRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Service for accessing and manipulating user data.
 *
 * This class is part of the skeleton project provided for students of the
 * course "Software Engineering" offered by Innsbruck University.
 */
@Service
public class UserxService implements UserDetailsService {
 
    private final UserxRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticatedUserService authenticatedUserService;
    private final DepartmentRepository departmentRepository;

    @Autowired
    public UserxService(UserxRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        AuthenticatedUserService authenticatedUserService,
                        DepartmentRepository departmentRepository,
                        EmployeeProfileRepository employeeProfileRepository)
    {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticatedUserService = authenticatedUserService;
        this.departmentRepository = departmentRepository;
    }
    
    /**
     * Returns a collection of all users.
     *
     * @return the userx collection
     */
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public Collection<Userx> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Loads a single user identified by its id.
     *
     * @param id the id to search for
     * @return the user with the id
     */
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public Optional<Userx> loadUser(Long id) {
        return userRepository.findById(id);
    }

    public Userx getUserById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException("User with id " + id + " not found"));
    }

    /**
     * Saves the user. This method will also set {@link Userx#createDate} for new
     * entities or {@link Userx#updateDate} for updated entities. The user
     * requesting this operation will also be stored as {@link Userx#createDate}
     * or {@link Userx#updateUser} respectively.
     *
     * @param user the user to save
     * @return the updated user
     */
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public Userx saveUser(Userx user) {
        if (user.isNew()) {
            if (userRepository.existsByUsername(user.getUsername())) {
                throw new UsernameDuplicateException("Username " + user.getUsername() + " not available");
            }
            user.setEnabled(true);
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            user.setCreateUser(authenticatedUserService.getAuthenticatedUser());
        } else {
            user.setUpdateUser(authenticatedUserService.getAuthenticatedUser());
        }
        return userRepository.save(user);
    }

    /**
     * Deletes the user.
     *
     * @param user the user to delete
     */
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public void deleteUser(Userx user) {
        Optional<Userx> userOpt = userRepository.findById(user.getId());
        userOpt.ifPresent(userRepository::delete);
    }

    // The following are self-service operations (no system_admin)

    public Userx getUserByUsername(String username) {
        return userRepository.findFirstByUsername(username).orElse(null);
    }


    /**
     * Loads a user by its username. Required for JWT authentication.
     *
     * @param username the username identifying the user whose data is required.
     * @return the user with the given username and their details.
     * @throws UsernameNotFoundException
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findFirstByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public Userx saveCurrentUser(UserxSelfUpdateDTO dto) {
        Userx user = authenticatedUserService.getAuthenticatedUser();

        if (dto.email() != null) user.setEmail(dto.email());
        if (dto.firstName() != null) user.setFirstName(dto.firstName());
        if (dto.lastName() != null) user.setLastName(dto.lastName());
        if (dto.phone() != null) user.setPhone(dto.phone());

        user.setUpdateUser(user);
        return userRepository.save(user);
    }

    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public Userx updateUser(Long id, UserxUpdateDTO dto) {
        Userx user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User with id " + id + " not found"));

        if (dto.firstName() != null) user.setFirstName(dto.firstName());
        if (dto.lastName() != null) user.setLastName(dto.lastName());
        if (dto.email() != null) user.setEmail(dto.email());
        if (dto.phone() != null) user.setPhone(dto.phone());
        if (dto.roles() != null) user.setRoles(dto.roles());
        if (dto.enabled() != null && dto.enabled()) user.setEnabled(true);

        if (dto.enabled() != null && !dto.enabled()) {
            user.setEnabled(false);
            List<Department> ledDepartments = departmentRepository.findByDepartmentLeader(user);

            for (Department dept : ledDepartments) {
                dept.setDepartmentLeader(null);
                departmentRepository.save(dept);
            }

            if (user.getEmployeeProfile() != null) {
                user.getEmployeeProfile().setUser(null);
                user.setEmployeeProfile(null);
            }

            user.getAbsences().clear();
        }

        user.setUpdateUser(authenticatedUserService.getAuthenticatedUser());
        return userRepository.save(user);
    }

    public void deleteCurrentUser() {
        Userx authenticatedUser = authenticatedUserService.getAuthenticatedUser();
        userRepository.delete(authenticatedUser);
    }
}

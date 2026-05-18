package at.qe.skeleton.services;

import at.qe.skeleton.models.Department;
import at.qe.skeleton.repositories.DepartmentRepository;
import at.qe.skeleton.common.exceptions.ConflictException;
import at.qe.skeleton.common.exceptions.NotFoundException;
import at.qe.skeleton.dtos.UserxSelfUpdateDTO;
import at.qe.skeleton.dtos.UserxUpdateDTO;
import at.qe.skeleton.models.Userx;
import at.qe.skeleton.repositories.UserxRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Service for accessing and manipulating user data.
 * This class is part of the skeleton project provided for students of the
 * course "Software Engineering" offered by Innsbruck University.
 */
@Slf4j
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
                        DepartmentRepository departmentRepository)
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
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("User with id " + id + " not found"));
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
                throw new ConflictException("Username " + user.getUsername() + " not available");
            }
            user.setEnabled(true);
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            user.setCreateUser(authenticatedUserService.getAuthenticatedUser());

            Userx savedUser = userRepository.save(user);
            log.info("Created user with id={} and username={}", savedUser.getId(), savedUser.getUsername());
            log.debug("User details: id={}, username={}, enabled={}, roles={}, createUserId={}",
                    savedUser.getId(),
                    savedUser.getUsername(),
                    savedUser.isEnabled(),
                    savedUser.getRoles(),
                    savedUser.getCreateUser() != null ? savedUser.getCreateUser().getId() : null);
            return savedUser;
        } else {
            user.setUpdateUser(authenticatedUserService.getAuthenticatedUser());

            Userx updatedUser = userRepository.save(user);
            log.info("Saved existing user with id={} and username={}", updatedUser.getId(), updatedUser.getUsername());
            log.debug("User details: id={}, username={}, enabled={}, roles={}, updateUserId={}",
                    updatedUser.getId(),
                    updatedUser.getUsername(),
                    updatedUser.isEnabled(),
                    updatedUser.getRoles(),
                    updatedUser.getUpdateUser() != null ? updatedUser.getUpdateUser().getId() : null);
            return updatedUser;
        }
    }

    /**
     * Deletes the user.
     *
     * @param user the user to delete
     */
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public void deleteUser(Userx user) {
        Optional<Userx> userOpt = userRepository.findById(user.getId());
        userOpt.ifPresent(existingUser -> {
            userRepository.delete(existingUser);
            log.info("Deleted user with id={} and username={}", existingUser.getId(), existingUser.getUsername());
        });
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
     * @throws UsernameNotFoundException if the user is not found.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserDetails userDetails = userRepository.findFirstByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        log.debug("Loaded user by username={}", username);

        return userDetails;
    }

    public Userx saveCurrentUser(UserxSelfUpdateDTO dto) {
        Userx user = authenticatedUserService.getAuthenticatedUser();

        StringBuilder debugInfo = new StringBuilder("Updated current user details:")
                .append(" id=").append(user.getId());

        if (dto.email() != null) {
            user.setEmail(dto.email());
            debugInfo.append(", email=").append(dto.email());
        }
        if (dto.firstName() != null) {
            user.setFirstName(dto.firstName());
            debugInfo.append(", firstName=").append(dto.firstName());
        }
        if (dto.lastName() != null) {
            user.setLastName(dto.lastName());
            debugInfo.append(", lastName=").append(dto.lastName());
        }
        if (dto.phone() != null) {
            user.setPhone(dto.phone());
            debugInfo.append(", phone=").append(dto.phone());
        }

        user.setUpdateUser(user);
        Userx updatedUser = userRepository.save(user);

        log.info("Updated current user with id={}", user.getId());
        log.debug(debugInfo.toString());

        return updatedUser;
    }

    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @Transactional
    public Userx updateUser(Long id, UserxUpdateDTO dto) {
        Userx user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User with id " + id + " not found"));

        StringBuilder debugInfo = new StringBuilder("Updated user details:")
                .append(" id=").append(id);

        if (dto.firstName() != null) {
            user.setFirstName(dto.firstName());
            debugInfo.append(", firstName=").append(dto.firstName());
        }
        if (dto.lastName() != null) {
            user.setLastName(dto.lastName());
            debugInfo.append(", lastName=").append(dto.lastName());
        }
        if (dto.email() != null) {
            user.setEmail(dto.email());
            debugInfo.append(", email=").append(dto.email());
        }
        if (dto.phone() != null) {
            user.setPhone(dto.phone());
            debugInfo.append(", phone=").append(dto.phone());
        }
        if (dto.roles() != null) {
            user.setRoles(dto.roles());
            debugInfo.append(", roles=").append(dto.roles());
        }
        if (dto.enabled() != null && dto.enabled()) {
            user.setEnabled(true);
            debugInfo.append(", enabled=true");
        }

        if (dto.enabled() != null && !dto.enabled()) {
            user.setEnabled(false);
            debugInfo.append(", enabled=false");
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
            debugInfo.append(", clearedLedDepartments=").append(ledDepartments.size())
                    .append(", clearedEmployeeProfileAndAbsences=true");
        }

        user.setUpdateUser(authenticatedUserService.getAuthenticatedUser());
        Userx updatedUser = userRepository.save(user);

        log.info("Updated user with id={}", id);
        log.debug(debugInfo.toString());

        return updatedUser;
    }

    public void deleteCurrentUser() {
        Userx authenticatedUser = authenticatedUserService.getAuthenticatedUser();
        userRepository.delete(authenticatedUser);

        log.info("Deleted current user with id={} and username={}",
                authenticatedUser.getId(),
                authenticatedUser.getUsername());
    }
}

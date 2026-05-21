package at.qe.skeleton.controllers;

import at.qe.skeleton.dtos.AbsenceDTO;
import at.qe.skeleton.mappers.AbsenceMapper;
import at.qe.skeleton.models.Absence;
import at.qe.skeleton.services.AbsenceService;
import at.qe.skeleton.services.AuthenticatedUserService;
import at.qe.skeleton.dtos.UserxDTO;
import at.qe.skeleton.dtos.UserxSelfUpdateDTO;
import at.qe.skeleton.mappers.UserxMapper;
import at.qe.skeleton.models.Userx;
import at.qe.skeleton.services.UserxService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;

/**
 * Userx endpoints exposed by the server.
 * This class is part of the skeleton project provided for students of the
* course "Software Engineering" offered by Innsbruck University.
 */
@RestController
@RequestMapping("/api/userx")
public class UserxController {
 
    private final UserxMapper userMapper;
    private final AuthenticatedUserService authenticatedUserService;
    private final UserxService userxService;
    private final AbsenceMapper absenceMapper;
    private final AbsenceService absenceService;

    @Autowired
    public UserxController(UserxMapper userMapper,
                           AuthenticatedUserService authenticatedUserService,
                           UserxService userxService,
                           AbsenceMapper absenceMapper,
                           AbsenceService absenceService) {
        this.userMapper = userMapper;
        this.authenticatedUserService = authenticatedUserService;
        this.userxService = userxService;
        this.absenceMapper = absenceMapper;
        this.absenceService = absenceService;
    }

    @Operation(summary = "Get current user",
            description = "Get the currently authenticated user.")
    @ApiResponse(responseCode = "200", description = "The currently authenticated user.")
    @ApiResponse(responseCode = "401", description = "User not authenticated.")
    @ApiResponse(responseCode = "403", description = "User not authorized.")
    @GetMapping("/me")
    public ResponseEntity<UserxDTO> getCurrentUser() {
        Userx authenticatedUser = authenticatedUserService.getAuthenticatedUser();
        return ResponseEntity.ok(userMapper.mapTo(authenticatedUser));
    }
    @Operation(summary = "Check if user is authenticated",
            description = "Check if the user is authenticated.")
    @GetMapping("/authenticated")
    public ResponseEntity<String> isAuthenticated(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body("User not authenticated");
        }
        return ResponseEntity.ok("User is authenticated: " + userDetails.getUsername());
        
    }

    @PatchMapping("/me")
    public ResponseEntity<UserxDTO> updateCurrentUser(@Valid @RequestBody UserxSelfUpdateDTO userxUpdateDto) {
        Userx saved = userxService.saveCurrentUser(userxUpdateDto);
        return ResponseEntity.ok(userMapper.mapTo(saved));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteCurrentUser() {
        userxService.deleteCurrentUser();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/absences")
    public ResponseEntity<Collection<AbsenceDTO>> getCurrentUserAbsences() {
        Userx authenticatedUser = authenticatedUserService.getAuthenticatedUser();
        Collection<Absence> absences = absenceService.getAbsencesForUser(authenticatedUser);
        List<AbsenceDTO> absenceDTOs = absences.stream()
                .map(absenceMapper::mapTo)
                .toList();
        return ResponseEntity.ok(absenceDTOs);
    }
}

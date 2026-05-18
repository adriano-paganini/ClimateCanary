package at.qe.skeleton.tests.services;

import at.qe.skeleton.common.exceptions.NotFoundException;
import at.qe.skeleton.dtos.AbsenceUpdateDTO;
import at.qe.skeleton.models.Absence;
import at.qe.skeleton.models.Userx;
import at.qe.skeleton.models.UserxRole;
import at.qe.skeleton.repositories.AbsenceRepository;
import at.qe.skeleton.services.AbsenceService;
import at.qe.skeleton.services.AuthenticatedUserService;
import at.qe.skeleton.services.UserxService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class AbsenceServiceTest {

    @Mock
    private AbsenceRepository absenceRepository;

    @Mock
    private UserxService userxService;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @InjectMocks
    private AbsenceService absenceService;

    private Userx adminUser;
    private Userx regularUser;
    private Userx otherUser;
    private Absence absence;

    @BeforeEach
    void setUp(){
        adminUser = new Userx();
        ReflectionTestUtils.setField(adminUser, "id", 1L);
        adminUser.setRoles(Set.of(UserxRole.SYSTEM_ADMIN));

        regularUser = new Userx();
        ReflectionTestUtils.setField(regularUser, "id", 2L);
        regularUser.setRoles(Set.of(UserxRole.EMPLOYEE));

        otherUser = new Userx();
        ReflectionTestUtils.setField(otherUser, "id", 3L);
        otherUser.setRoles(Set.of(UserxRole.EMPLOYEE));

        absence = new Absence();
        absence.setUser(regularUser);
        absence.setStartDate(LocalDateTime.of(2025, 6, 1, 12, 0));
        absence.setEndDate(LocalDateTime.of(2025, 6, 5, 12, 0));
    }

    @Test
    @DisplayName("Get all absences without filters returns all")
    void getAll_noFilters_returnsAllAbsences(){
        Mockito.when(absenceRepository.findAll()).thenReturn(List.of(absence));
        List<Absence> result = absenceService.getAll(null, null);

        Assertions.assertThat(result).containsExactly(absence);
        Mockito.verify(absenceRepository).findAll();
        Mockito.verify(absenceRepository, Mockito.never()).search(Mockito.any(), Mockito.any());
    }

    @Test
    @DisplayName("Get all absences with user id calls search")
    void getAll_withUserxId_callsSearch() {
        Mockito.when(absenceRepository.search(2L, null)).thenReturn(List.of(absence));

        List<Absence> result = absenceService.getAll(2L, null);

        Assertions.assertThat(result).containsExactly(absence);
        Mockito.verify(absenceRepository).search(2L, null);
        Mockito.verify(absenceRepository, Mockito.never()).findAll();
    }

    @Test
    @DisplayName("Get all absences with department id calls search")
    void getAll_withDepartmentId_callsSearch() {
        Mockito.when(absenceRepository.search(null, 5L)).thenReturn(List.of(absence));

        List<Absence> result = absenceService.getAll(null, 5L);

        Assertions.assertThat(result).containsExactly(absence);
        Mockito.verify(absenceRepository).search(null, 5L);
    }

    @Test
    @DisplayName("Get all absences with both filters calls search")
    void getAll_withBothFilters_callsSearch() {
        Mockito.when(absenceRepository.search(2L, 5L)).thenReturn(List.of(absence));

        List<Absence> result = absenceService.getAll(2L, 5L);

        Assertions.assertThat(result).containsExactly(absence);
        Mockito.verify(absenceRepository).search(2L, 5L);
    }

    @Test
    @DisplayName("Get absence by id returns absence")
    void getById_existingId_returnsAbsence() {
        Mockito.when(absenceRepository.findById(10L)).thenReturn(Optional.of(absence));

        Absence result = absenceService.getById(10L);

        Assertions.assertThat(result).isEqualTo(absence);
    }

    @Test
    @DisplayName("Get absence by id throws when not found")
    void getById_nonExistingId_throwsNotFoundException() {
        Mockito.when(absenceRepository.findById(99L)).thenReturn(Optional.empty());

        Assertions.assertThatThrownBy(() -> absenceService.getById(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("99");
    }


    @Test
    @DisplayName("Get absences for null user throws not found")
    void getAbsencesForUser_nullUser_throwsNotFoundException() {
        Assertions.assertThatThrownBy(() -> absenceService.getAbsencesForUser(null))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Get absences for self returns absences")
    void getAbsencesForUser_selfAccess_returnsAbsences() {
        Mockito.when(authenticatedUserService.getAuthenticatedUser()).thenReturn(regularUser);
        Mockito.when(absenceRepository.findByUser(regularUser)).thenReturn(List.of(absence));

        var result = absenceService.getAbsencesForUser(regularUser);

        Assertions.assertThat(result).containsExactly(absence);
    }

    @Test
    @DisplayName("Get absences for admin accessing other user returns absences")
    void getAbsencesForUser_adminAccessOtherUser_returnsAbsences() {
        Mockito.when(authenticatedUserService.getAuthenticatedUser()).thenReturn(adminUser);
        Mockito.when(absenceRepository.findByUser(regularUser)).thenReturn(List.of(absence));

        var result = absenceService.getAbsencesForUser(regularUser);

        Assertions.assertThat(result).containsExactly(absence);
    }

    @Test
    @DisplayName("Get absences for non admin accessing other user throws access denied")
    void getAbsencesForUser_nonAdminAccessOtherUser_throwsAccessDeniedException() {
        Mockito.when(authenticatedUserService.getAuthenticatedUser()).thenReturn(otherUser);

        Assertions.assertThatThrownBy(() -> absenceService.getAbsencesForUser(regularUser))
                .isInstanceOf(AccessDeniedException.class);

        Mockito.verify(absenceRepository, Mockito.never()).findByUser(Mockito.any());
    }


    @Test
    @DisplayName("Create absence with valid dates saves and returns")
    void create_validDates_savesAndReturnsAbsence() {
        Mockito.when(absenceRepository.save(absence)).thenReturn(absence);

        Absence result = absenceService.create(absence);

        Assertions.assertThat(result).isEqualTo(absence);
        Mockito.verify(absenceRepository).save(absence);
    }

    @Test
    @DisplayName("Create absence with end date before start date throws")
    void create_endDateBeforeStartDate_throwsIllegalArgumentException() {
        absence.setStartDate(LocalDateTime.of(2025, 6, 10, 12, 0));
        absence.setEndDate(LocalDateTime.of(2025, 6, 1, 12, 0));

        Assertions.assertThatThrownBy(() -> absenceService.create(absence))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("end date");

        Mockito.verify(absenceRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    @DisplayName("Create absence with equal start and end date succeeds")
    void create_endDateEqualsStartDate_savesSuccessfully() {
        absence.setStartDate(LocalDateTime.of(2025, 6, 5, 12, 0));
        absence.setEndDate(LocalDateTime.of(2025, 6, 5, 12, 0));
        Mockito.when(absenceRepository.save(absence)).thenReturn(absence);

        Assertions.assertThatCode(() -> absenceService.create(absence)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Create absence with null start date succeeds")
    void create_nullStartDate_savesSuccessfully() {
        absence.setStartDate(null);
        Mockito.when(absenceRepository.save(absence)).thenReturn(absence);

        Assertions.assertThatCode(() -> absenceService.create(absence)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Create absence with null end date succeeds")
    void create_nullEndDate_savesSuccessfully() {
        absence.setEndDate(null);
        Mockito.when(absenceRepository.save(absence)).thenReturn(absence);

        Assertions.assertThatCode(() -> absenceService.create(absence)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Create absence with both dates null succeeds")
    void create_bothDatesNull_savesSuccessfully() {
        absence.setStartDate(null);
        absence.setEndDate(null);
        Mockito.when(absenceRepository.save(absence)).thenReturn(absence);

        Assertions.assertThatCode(() -> absenceService.create(absence)).doesNotThrowAnyException();
        Mockito.verify(absenceRepository).save(absence);
    }

    @Test
    @DisplayName("Update absence with all fields updates and returns")
    void update_allFieldsProvided_updatesAndReturnsAbsence() {
        Userx newUser = new Userx();
        ReflectionTestUtils.setField(newUser, "id", 5L);

        AbsenceUpdateDTO dto = new AbsenceUpdateDTO(
                LocalDateTime.of(2025, 7, 1, 12, 0),
                LocalDateTime.of(2025, 7, 5, 12, 0),
                absence.getAbsenceType(),
                newUser.getId(),
                absence.getAbsenceStatus()
        );

        Mockito.when(absenceRepository.findById(10L)).thenReturn(Optional.of(absence));
        Mockito.when(authenticatedUserService.getAuthenticatedUser()).thenReturn(adminUser);
        Mockito.when(userxService.getUserById(5L)).thenReturn(newUser);
        Mockito.when(absenceRepository.save(Mockito.any(Absence.class)))
                .thenReturn(absence);

        Absence result = absenceService.update(10L, dto);

        Assertions.assertThat(result.getUser()).isEqualTo(newUser);
        Assertions.assertThat(result.getStartDate()).isEqualTo(LocalDateTime.of(2025, 7, 1, 12, 0));
        Assertions.assertThat(result.getEndDate()).isEqualTo(LocalDateTime.of(2025, 7, 5, 12, 0));
        Mockito.verify(absenceRepository).save(absence);
    }

    @Test
    @DisplayName("Update absence with only start date changes start date")
    void update_onlyStartDateProvided_onlyStartDateChanged() {
        LocalDateTime originalEnd = absence.getEndDate();
        Userx originalUser = absence.getUser();

        AbsenceUpdateDTO dto = new AbsenceUpdateDTO(LocalDateTime.of(2025, 2, 1, 12, 5), null, null, null, null);

        Mockito.when(absenceRepository.findById(10L)).thenReturn(Optional.of(absence));
        Mockito.when(authenticatedUserService.getAuthenticatedUser()).thenReturn(adminUser);
        Mockito.when(absenceRepository.save(absence)).thenReturn(absence);

        absenceService.update(10L, dto);

        Assertions.assertThat(absence.getUser()).isEqualTo(originalUser);
        Assertions.assertThat(absence.getStartDate()).isEqualTo(LocalDateTime.of(2025, 2, 1, 12, 5));
        Assertions.assertThat(absence.getEndDate()).isEqualTo(originalEnd);
        Mockito.verify(userxService, Mockito.never()).getUserById(Mockito.any());
    }

    @Test
    @DisplayName("Update absence with empty dto does not change anything")
    void update_emptyDto_nothingChanged() {
        Userx originalUser = absence.getUser();
        LocalDateTime originalStart = absence.getStartDate();
        LocalDateTime originalEnd = absence.getEndDate();

        AbsenceUpdateDTO dto = new AbsenceUpdateDTO(null, null, null, null, null);

        Mockito.when(absenceRepository.findById(10L)).thenReturn(Optional.of(absence));
        Mockito.when(authenticatedUserService.getAuthenticatedUser()).thenReturn(adminUser);
        Mockito.when(absenceRepository.save(absence)).thenReturn(absence);

        absenceService.update(10L, dto);

        Assertions.assertThat(absence.getUser()).isEqualTo(originalUser);
        Assertions.assertThat(absence.getStartDate()).isEqualTo(originalStart);
        Assertions.assertThat(absence.getEndDate()).isEqualTo(originalEnd);
    }

    @Test
    @DisplayName("Update absence with non existing id throws not found")
    void update_nonExistingId_throwsNotFoundException() {
        AbsenceUpdateDTO dto = new AbsenceUpdateDTO(null, null, null, null, null);
        Mockito.when(absenceRepository.findById(99L)).thenReturn(Optional.empty());

        Assertions.assertThatThrownBy(() -> absenceService.update(99L, dto))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("99");

        Mockito.verify(absenceRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    @DisplayName("Delete absence with existing id calls delete")
    void delete_existingId_callsDeleteById() {
        Mockito.when(absenceRepository.findById(10L)).thenReturn(Optional.of(absence));

        absenceService.delete(10L);

        Mockito.verify(absenceRepository).deleteById(10L);
    }

    @Test
    @DisplayName("Delete absence with non existing id throws not found")
    void delete_nonExistingId_throwsNotFoundException() {
        Mockito.when(absenceRepository.findById(99L)).thenReturn(Optional.empty());

        Assertions.assertThatThrownBy(() -> absenceService.delete(99L))
                .isInstanceOf(NotFoundException.class);

        Mockito.verify(absenceRepository, Mockito.never()).deleteById(Mockito.any());
    }
}

package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.AbsenceDTO;
import at.qe.skeleton.mappers.AbsenceMapper;
import at.qe.skeleton.models.*;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

class AbsenceMapperTest {

    private final AbsenceMapper mapper = new AbsenceMapper();

    @Test
    void mapTo_shouldMapAllFields() {
        Userx user = new Userx();
        ReflectionTestUtils.setField(user, "id", 99L);
        user.setUsername("john");
        user.setFirstName("John");
        user.setLastName("Doe");

        Absence absence = new Absence();
        ReflectionTestUtils.setField(absence, "id", 1L);

        absence.setStartDate(LocalDateTime.now());
        absence.setEndDate(LocalDateTime.now().plusDays(1));
        absence.setAbsenceType(AbsenceType.HOLIDAY);
        absence.setAbsenceStatus(AbsenceStatus.APPROVED);
        absence.setUser(user);

        AbsenceDTO dto = mapper.mapTo(absence);

        Assertions.assertThat(dto.id()).isEqualTo(1L);
        Assertions.assertThat(dto.startDate()).isEqualTo(absence.getStartDate());
        Assertions.assertThat(dto.endDate()).isEqualTo(absence.getEndDate());
        Assertions.assertThat(dto.absenceType()).isEqualTo(AbsenceType.HOLIDAY);
        Assertions.assertThat(dto.absenceStatus()).isEqualTo(AbsenceStatus.APPROVED);
        Assertions.assertThat(dto.userxId()).isEqualTo(99L);
        Assertions.assertThat(dto.username()).isEqualTo("john");
        Assertions.assertThat(dto.userFirstName()).isEqualTo("John");
        Assertions.assertThat(dto.userLastName()).isEqualTo("Doe");
    }

    @Test
    void mapFrom_shouldThrowException() {
        AbsenceDTO dto = new AbsenceDTO(
                1L,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                AbsenceType.SICKNESS,
                AbsenceStatus.PLANNED,
                99L,
                "john",
                "John",
                "Doe"
        );

        Assertions.assertThatThrownBy(() -> mapper.mapFrom(dto))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}

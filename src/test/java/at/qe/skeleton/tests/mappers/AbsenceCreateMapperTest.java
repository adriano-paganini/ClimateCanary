package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.AbsenceCreateDTO;
import at.qe.skeleton.mappers.AbsenceCreateMapper;
import at.qe.skeleton.models.*;
import at.qe.skeleton.services.UserxService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;

@ExtendWith(MockitoExtension.class)
class AbsenceCreateMapperTest {

    @Mock
    private UserxService userxService;

    @InjectMocks
    private AbsenceCreateMapper mapper;

    @Test
    void mapFrom_shouldMapAllFields() {
        Userx user = new Userx();

        Mockito.when(userxService.getUserById(99L))
                .thenReturn(user);

        AbsenceCreateDTO dto = new AbsenceCreateDTO(
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(2),
                AbsenceType.HOLIDAY,
                99L
        );

        Absence result = mapper.mapFrom(dto);

        Assertions.assertThat(result.getUser()).isEqualTo(user);
        Assertions.assertThat(result.getStartDate()).isEqualTo(dto.startDate());
        Assertions.assertThat(result.getEndDate()).isEqualTo(dto.endDate());
        Assertions.assertThat(result.getAbsenceType()).isEqualTo(dto.absenceType());

        Assertions.assertThat(result.getAbsenceStatus())
                .isEqualTo(AbsenceStatus.PLANNED);

        Mockito.verify(userxService).getUserById(99L);
    }

    @Test
    void mapTo_shouldThrowException() {
        Absence absence = new Absence();

        Assertions.assertThatThrownBy(() -> mapper.mapTo(absence))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
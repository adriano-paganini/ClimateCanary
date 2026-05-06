package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.EmployeeProfileDTO;
import at.qe.skeleton.mappers.EmployeeProfileMapper;
import at.qe.skeleton.models.*;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class EmployeeProfileMapperTest {

    private final EmployeeProfileMapper mapper = new EmployeeProfileMapper();

    @Test
    void mapTo_shouldMapAllFields() {
        Userx user = new Userx();
        ReflectionTestUtils.setField(user, "id", 99L);

        Department department = new Department();
        ReflectionTestUtils.setField(department, "id", 10L);

        Room room = new Room();
        ReflectionTestUtils.setField(room, "id", 20L);

        EmployeeProfile profile = new EmployeeProfile();
        ReflectionTestUtils.setField(profile, "id", 1L);

        profile.setUser(user);
        profile.setDepartment(department);
        profile.setRoom(room);

        EmployeeProfileDTO dto = mapper.mapTo(profile);

        Assertions.assertThat(dto.id()).isEqualTo(1L);
        Assertions.assertThat(dto.userxId()).isEqualTo(99L);
        Assertions.assertThat(dto.departmentId()).isEqualTo(10L);
        Assertions.assertThat(dto.roomId()).isEqualTo(20L);
    }

    @Test
    void mapTo_shouldHandleNullRelations() {
        Userx user = new Userx();
        ReflectionTestUtils.setField(user, "id", 99L);

        EmployeeProfile profile = new EmployeeProfile();
        profile.setUser(user);
        profile.setDepartment(null);
        profile.setRoom(null);

        EmployeeProfileDTO dto = mapper.mapTo(profile);

        Assertions.assertThat(dto.departmentId()).isNull();
        Assertions.assertThat(dto.roomId()).isNull();
    }

    @Test
    void mapFrom_shouldThrowException() {
        EmployeeProfileDTO dto = new EmployeeProfileDTO(
                1L,
                99L,
                null,
                null,
                null,
                null
        );

        Assertions.assertThatThrownBy(() -> mapper.mapFrom(dto))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
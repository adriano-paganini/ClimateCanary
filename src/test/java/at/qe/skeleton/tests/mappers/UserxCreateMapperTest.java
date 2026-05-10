package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.UserxCreateDTO;
import at.qe.skeleton.mappers.UserxCreateMapper;
import at.qe.skeleton.models.Userx;
import at.qe.skeleton.models.UserxRole;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;

class UserxCreateMapperTest {

    private final UserxCreateMapper mapper = new UserxCreateMapper();

    @Test
    void mapFrom_shouldMapAllFields() {
        UserxCreateDTO dto = new UserxCreateDTO(
                "john.doe",
                "secret",
                "John",
                "Doe",
                "john@example.com",
                "123456",
                true,
                Set.of(UserxRole.EMPLOYEE)
        );

        Userx result = mapper.mapFrom(dto);

        Assertions.assertThat(result.getUsername()).isEqualTo("john.doe");
        Assertions.assertThat(result.getPassword()).isEqualTo("secret");
        Assertions.assertThat(result.getFirstName()).isEqualTo("John");
        Assertions.assertThat(result.getLastName()).isEqualTo("Doe");
        Assertions.assertThat(result.getEmail()).isEqualTo("john@example.com");
        Assertions.assertThat(result.getPhone()).isEqualTo("123456");
        Assertions.assertThat(result.isEnabled()).isTrue();
        Assertions.assertThat(result.getRoles()).containsExactly(UserxRole.EMPLOYEE);
        Assertions.assertThat(result.getId()).isNull();
    }

    @Test
    void mapTo_shouldThrowException() {
        Userx entity = new Userx();

        Assertions.assertThatThrownBy(() -> mapper.mapTo(entity))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
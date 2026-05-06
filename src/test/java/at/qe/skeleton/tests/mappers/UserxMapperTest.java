package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.UserxDTO;
import at.qe.skeleton.mappers.UserxMapper;
import at.qe.skeleton.models.Userx;
import at.qe.skeleton.models.UserxRole;
import at.qe.skeleton.services.UserxService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
public class UserxMapperTest {

    @Mock
    private UserxService userxService;

    @InjectMocks
    private UserxMapper mapper;

    @Test
    void mapTo_shouldMapAllFields() {
        Userx creator = new Userx();
        ReflectionTestUtils.setField(creator, "id", 10L);

        Userx updater = new Userx();
        ReflectionTestUtils.setField(updater, "id", 20L);

        Userx user = new Userx();
        ReflectionTestUtils.setField(user, "id", 1L);

        user.setCreateUser(creator);
        user.setCreateDate(LocalDateTime.now());
        user.setUpdateUser(updater);
        user.setUpdateDate(LocalDateTime.now());
        user.setUsername("john");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john@example.com");
        user.setPhone("123");
        user.setEnabled(true);
        user.setRoles(Set.of(UserxRole.EMPLOYEE));

        UserxDTO dto = mapper.mapTo(user);

        Assertions.assertThat(dto.id()).isEqualTo(1L);
        Assertions.assertThat(dto.updatedBy()).isEqualTo(20L);
        Assertions.assertThat(dto.username()).isEqualTo("john");
        Assertions.assertThat(dto.roles()).contains(UserxRole.EMPLOYEE);
    }

    @Test
    void mapTo_shouldHandleNullUpdateUser() {
        Userx creator = new Userx();
        ReflectionTestUtils.setField(creator, "id", 10L);

        Userx user = new Userx();
        user.setCreateUser(creator);
        user.setUpdateUser(null);

        UserxDTO dto = mapper.mapTo(user);

        Assertions.assertThat(dto.updatedBy()).isNull();
    }

    @Test
    void mapTo_shouldReturnNull_whenInputNull() {
        Assertions.assertThat(mapper.mapTo(null)).isNull();
    }

    @Test
    void mapFrom_shouldReturnNull_whenDtoNull() {
        Assertions.assertThat(mapper.mapFrom(null)).isNull();
    }

    @Test
    void mapFrom_shouldLoadExistingUser_whenIdPresent() {
        Userx existing = new Userx();
        ReflectionTestUtils.setField(existing, "id", 1L);

        Mockito.when(userxService.loadUser(1L))
                .thenReturn(Optional.of(existing));

        UserxDTO dto = new UserxDTO(
                1L, null, null, null, null,
                "john", "John", "Doe",
                "mail", "123", true,
                Set.of(UserxRole.SYSTEM_ADMIN)
        );

        Userx result = mapper.mapFrom(dto);

        Assertions.assertThat(result).isSameAs(existing);
        Assertions.assertThat(result.getFirstName()).isEqualTo("John");
        Assertions.assertThat(result.getRoles()).contains(UserxRole.SYSTEM_ADMIN);
    }

    @Test
    void mapFrom_shouldCreateNew_whenServiceReturnsEmpty() {
        Mockito.when(userxService.loadUser(1L))
                .thenReturn(Optional.empty());

        UserxDTO dto = new UserxDTO(
                1L, null, null, null, null,
                "john", "John", "Doe",
                "mail", "123", true,
                Set.of()
        );

        Userx result = mapper.mapFrom(dto);

        Assertions.assertThat(result.getId()).isNull();
        Assertions.assertThat(result.getFirstName()).isEqualTo("John");
    }

    @Test
    void mapFrom_shouldCreateNew_whenIdNull() {
        UserxDTO dto = new UserxDTO(
                null, null, null, null, null,
                "john", "John", "Doe",
                "mail", "123", true,
                Set.of()
        );

        Userx result = mapper.mapFrom(dto);

        Assertions.assertThat(result.getFirstName()).isEqualTo("John");
        Assertions.assertThat(result.getId()).isNull();
    }
}

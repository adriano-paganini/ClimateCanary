package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.RoomDTO;
import at.qe.skeleton.mappers.RoomMapper;
import at.qe.skeleton.models.*;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class RoomMapperTest {

    private final RoomMapper mapper = new RoomMapper();

    @Test
    void mapTo_shouldMapAllFields() {
        Department department = new Department();
        ReflectionTestUtils.setField(department, "id", 10L);

        Building building = new Building();
        ReflectionTestUtils.setField(building, "id", 20L);

        Room room = new Room();
        ReflectionTestUtils.setField(room, "id", 1L);

        room.setName("Conference Room");
        room.setRoomType(RoomType.OFFICE);
        room.setPrivacyMode(true);
        room.setActive(true);
        room.setDepartment(department);
        room.setBuilding(building);

        RoomDTO dto = mapper.mapTo(room);

        Assertions.assertThat(dto.id()).isEqualTo(1L);
        Assertions.assertThat(dto.name()).isEqualTo("Conference Room");
        Assertions.assertThat(dto.roomType()).isEqualTo(RoomType.OFFICE);
        Assertions.assertThat(dto.privacyMode()).isTrue();
        Assertions.assertThat(dto.departmentId()).isEqualTo(10L);
        Assertions.assertThat(dto.buildingId()).isEqualTo(20L);
        Assertions.assertThat(dto.active()).isTrue();
    }

    @Test
    void mapTo_shouldHandleNullRelations() {
        Room room = new Room();
        room.setName("Simple Room");
        room.setRoomType(RoomType.OFFICE);
        room.setPrivacyMode(true);
        room.setActive(false);

        RoomDTO dto = mapper.mapTo(room);

        Assertions.assertThat(dto.departmentId()).isNull();
        Assertions.assertThat(dto.buildingId()).isNull();
    }

    @Test
    void mapFrom_shouldThrowException() {
        RoomDTO dto = new RoomDTO(
                1L,
                "Test Room",
                RoomType.OFFICE,
                true,
                null,
                null,
                true
        );

        Assertions.assertThatThrownBy(() -> mapper.mapFrom(dto))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
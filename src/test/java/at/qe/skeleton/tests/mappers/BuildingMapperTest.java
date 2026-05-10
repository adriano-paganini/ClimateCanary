package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.BuildingDTO;
import at.qe.skeleton.mappers.BuildingMapper;
import at.qe.skeleton.models.Address;
import at.qe.skeleton.models.Building;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class BuildingMapperTest {

    private final BuildingMapper mapper = new BuildingMapper();

    @Test
    void mapTo_shouldMapAllFields() {
        Address address = new Address();
        ReflectionTestUtils.setField(address, "id", 10L);

        Building building = new Building();
        ReflectionTestUtils.setField(building, "id", 1L);
        building.setName("Main Building");
        building.setAddress(address);

        BuildingDTO dto = mapper.mapTo(building);

        Assertions.assertThat(dto.id()).isEqualTo(1L);
        Assertions.assertThat(dto.name()).isEqualTo("Main Building");
        Assertions.assertThat(dto.addressId()).isEqualTo(10L);
    }

    @Test
    void mapTo_shouldHandleNullAddress() {
        Building building = new Building();
        building.setName("No Address Building");

        BuildingDTO dto = mapper.mapTo(building);

        Assertions.assertThat(dto.addressId()).isNull();
    }


    @Test
    void mapFrom_shouldMapNameOnly() {
        BuildingDTO dto = new BuildingDTO(
                1L,
                "Mapped Building",
                10L
        );

        Building building = mapper.mapFrom(dto);

        Assertions.assertThat(building.getName()).isEqualTo("Mapped Building");

        Assertions.assertThat(building.getId()).isNull();
        Assertions.assertThat(building.getAddress()).isNull();
    }
}
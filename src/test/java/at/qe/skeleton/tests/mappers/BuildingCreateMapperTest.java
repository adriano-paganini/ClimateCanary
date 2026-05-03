package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.BuildingCreateDTO;
import at.qe.skeleton.mappers.BuildingCreateMapper;
import at.qe.skeleton.models.Address;
import at.qe.skeleton.models.Building;
import at.qe.skeleton.services.AddressService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BuildingCreateMapperTest {

    @Mock
    private AddressService addressService;

    @InjectMocks
    private BuildingCreateMapper mapper;

    @Test
    void mapFrom_shouldMapAllFields() {
        Address address = new Address();

        Mockito.when(addressService.getById(10L))
                .thenReturn(address);
gi
        BuildingCreateDTO dto = new BuildingCreateDTO(
                "Main Building",
                10L
        );

        Building result = mapper.mapFrom(dto);

        Assertions.assertThat(result.getName()).isEqualTo("Main Building");
        Assertions.assertThat(result.getAddress()).isEqualTo(address);
        Mockito.verify(addressService).getById(10L);
    }

    @Test
    void mapTo_shouldThrowException() {
        Building building = new Building();

        Assertions.assertThatThrownBy(() -> mapper.mapTo(building))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.AddressDTO;
import at.qe.skeleton.mappers.AddressMapper;
import at.qe.skeleton.models.Address;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AddressMapperTest {

    private final AddressMapper mapper = new AddressMapper();

    @Test
    void mapTo_shouldMapAllFields() {
        Address address = new Address();
        ReflectionTestUtils.setField(address, "id", 1L);

        address.setCountry("Austria");
        address.setZipCode("6020");
        address.setCity("Innsbruck");
        address.setStreet("Main Street");
        address.setHouseNumber("1A");
        address.setExtra("Top 3");

        AddressDTO dto = mapper.mapTo(address);

        Assertions.assertThat(dto.id()).isEqualTo(1L);
        Assertions.assertThat(dto.country()).isEqualTo("Austria");
        Assertions.assertThat(dto.zipCode()).isEqualTo("6020");
        Assertions.assertThat(dto.city()).isEqualTo("Innsbruck");
        Assertions.assertThat(dto.street()).isEqualTo("Main Street");
        Assertions.assertThat(dto.houseNumber()).isEqualTo("1A");
        Assertions.assertThat(dto.extra()).isEqualTo("Top 3");
    }

    @Test
    void mapFrom_shouldThrowException() {
        AddressDTO dto = new AddressDTO(
                1L,
                "Austria",
                "6020",
                "Innsbruck",
                "Main Street",
                "1A",
                "Top 3"
        );

        Assertions.assertThatThrownBy(() -> mapper.mapFrom(dto))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
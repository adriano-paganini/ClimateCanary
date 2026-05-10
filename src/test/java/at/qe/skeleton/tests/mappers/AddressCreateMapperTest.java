package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.AddressCreateDTO;
import at.qe.skeleton.mappers.AddressCreateMapper;
import at.qe.skeleton.models.Address;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class AddressCreateMapperTest {

    private final AddressCreateMapper mapper = new AddressCreateMapper();

    @Test
    void mapFrom_shouldMapAllFields() {
        AddressCreateDTO dto = new AddressCreateDTO(
                "Austria",
                "6020",
                "Innsbruck",
                "Main Street",
                "1A",
                "Top 3"
        );

        Address result = mapper.mapFrom(dto);

        Assertions.assertThat(result.getCountry()).isEqualTo("Austria");
        Assertions.assertThat(result.getZipCode()).isEqualTo("6020");
        Assertions.assertThat(result.getCity()).isEqualTo("Innsbruck");
        Assertions.assertThat(result.getStreet()).isEqualTo("Main Street");
        Assertions.assertThat(result.getHouseNumber()).isEqualTo("1A");
        Assertions.assertThat(result.getExtra()).isEqualTo("Top 3");
        Assertions.assertThat(result.getId()).isNull();
    }

    @Test
    void mapTo_shouldThrowException() {
        Address address = new Address();

        Assertions.assertThatThrownBy(() -> mapper.mapTo(address))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
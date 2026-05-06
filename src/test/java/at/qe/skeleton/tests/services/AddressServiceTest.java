package at.qe.skeleton.tests.services;

import at.qe.skeleton.common.exceptions.ConflictException;
import at.qe.skeleton.common.exceptions.NotFoundException;
import at.qe.skeleton.dtos.AddressUpdateDTO;
import at.qe.skeleton.models.Address;
import at.qe.skeleton.repositories.AddressRepository;
import at.qe.skeleton.repositories.BuildingRepository;
import at.qe.skeleton.services.AddressService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private BuildingRepository buildingRepository;

    @InjectMocks
    private AddressService addressService;

    private Address address;

    @BeforeEach
    void setUp() {
        address = new Address();
        address.setCountry("Austria");
        address.setZipCode("6020");
        address.setCity("Innsbruck");
        address.setStreet("Technikerstraße");
        address.setHouseNumber("21a");
        address.setExtra("Top 3");
    }

    @Test
    @DisplayName("Get all addresses returns all")
    void getAll_returnsAllAddresses() {
        Mockito.when(addressRepository.findAll()).thenReturn(List.of(address));

        List<Address> result = addressService.getAll();

        Assertions.assertThat(result).containsExactly(address);
        Mockito.verify(addressRepository).findAll();
    }

    @Test
    @DisplayName("Get all addresses empty returns empty list")
    void getAll_empty_returnsEmptyList() {
        Mockito.when(addressRepository.findAll()).thenReturn(List.of());

        List<Address> result = addressService.getAll();

        Assertions.assertThat(result).isEmpty();
    }


    @Test
    @DisplayName("Get address by id returns address")
    void getById_existingId_returnsAddress() {
        Mockito.when(addressRepository.findById(1L)).thenReturn(Optional.of(address));

        Address result = addressService.getById(1L);

        Assertions.assertThat(result).isEqualTo(address);
    }

    @Test
    @DisplayName("Get address by id throws when not found")
    void getById_missingId_throwsNotFoundException() {
        Mockito.when(addressRepository.findById(99L)).thenReturn(Optional.empty());

        Assertions.assertThatThrownBy(() -> addressService.getById(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("Create address with valid data saves and returns")
    void create_validAddress_savesAndReturns() {
        Mockito.when(addressRepository.save(address)).thenReturn(address);

        Address result = addressService.create(address);

        Assertions.assertThat(result).isEqualTo(address);
        Mockito.verify(addressRepository).save(address);
    }

    @Test
    @DisplayName("Create minimal address saves successfully")
    void create_minimalAddress_savesSuccessfully() {
        Address minimal = new Address();
        minimal.setCountry("Germany");
        minimal.setCity("Berlin");
        Mockito.when(addressRepository.save(minimal)).thenReturn(minimal);

        Address result = addressService.create(minimal);

        Assertions.assertThat(result).isEqualTo(minimal);
    }


    @Test
    @DisplayName("Update address with all fields updates all fields")
    void update_allFieldsProvided_updatesAllFields() {
        AddressUpdateDTO dto = new AddressUpdateDTO(
                "Germany", "10115", "Berlin",
                "Unter den Linden", "1", "Apt 2"
        );

        Mockito.when(addressRepository.findById(1L)).thenReturn(Optional.of(address));
        Mockito.when(addressRepository.save(Mockito.any(Address.class))).thenAnswer(inv -> inv.getArgument(0));

        Address result = addressService.update(1L, dto);

        Assertions.assertThat(result.getCountry()).isEqualTo("Germany");
        Assertions.assertThat(result.getZipCode()).isEqualTo("10115");
        Assertions.assertThat(result.getCity()).isEqualTo("Berlin");
        Assertions.assertThat(result.getStreet()).isEqualTo("Unter den Linden");
        Assertions.assertThat(result.getHouseNumber()).isEqualTo("1");
        Assertions.assertThat(result.getExtra()).isEqualTo("Apt 2");
    }

    @Test
    @DisplayName("Update address with no fields keeps existing values")
    void update_noFieldsProvided_keepsExistingValues() {
        AddressUpdateDTO dto = new AddressUpdateDTO(null, null, null, null, null, null);

        Mockito.when(addressRepository.findById(1L)).thenReturn(Optional.of(address));
        Mockito.when(addressRepository.save(Mockito.any(Address.class))).thenAnswer(inv -> inv.getArgument(0));

        Address result = addressService.update(1L, dto);

        Assertions.assertThat(result.getCountry()).isEqualTo("Austria");
        Assertions.assertThat(result.getZipCode()).isEqualTo("6020");
        Assertions.assertThat(result.getCity()).isEqualTo("Innsbruck");
        Assertions.assertThat(result.getStreet()).isEqualTo("Technikerstraße");
        Assertions.assertThat(result.getHouseNumber()).isEqualTo("21a");
        Assertions.assertThat(result.getExtra()).isEqualTo("Top 3");
    }

    @Test
    @DisplayName("Update address with partial fields updates only provided fields")
    void update_partialFields_onlyUpdatesProvidedFields() {
        AddressUpdateDTO dto = new AddressUpdateDTO(null, "10115", "Berlin", null, null, null);

        Mockito.when(addressRepository.findById(1L)).thenReturn(Optional.of(address));
        Mockito.when(addressRepository.save(Mockito.any(Address.class))).thenAnswer(inv -> inv.getArgument(0));

        Address result = addressService.update(1L, dto);

        Assertions.assertThat(result.getCountry()).isEqualTo("Austria"); // unchanged
        Assertions.assertThat(result.getZipCode()).isEqualTo("10115");   // updated
        Assertions.assertThat(result.getCity()).isEqualTo("Berlin");     // updated
        Assertions.assertThat(result.getStreet()).isEqualTo("Technikerstraße"); // unchanged
    }

    @Test
    @DisplayName("Update address with non existing id throws not found")
    void update_nonExistentId_throwsNotFoundException() {
        AddressUpdateDTO dto = new AddressUpdateDTO(null, null, null, null, null, null);
        Mockito.when(addressRepository.findById(99L)).thenReturn(Optional.empty());

        Assertions.assertThatThrownBy(() -> addressService.update(99L, dto))
                .isInstanceOf(NotFoundException.class);
    }


    @Test
    @DisplayName("Delete address not referenced by building deletes successfully")
    void delete_existingIdNotReferencedByBuilding_deletesSuccessfully() {
        Mockito.when(addressRepository.findById(1L)).thenReturn(Optional.of(address));
        Mockito.when(buildingRepository.existsBuildingByAddressId(1L)).thenReturn(false);

        addressService.delete(1L);

        Mockito.verify(addressRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Delete address referenced by building throws conflict")
    void delete_addressReferencedByBuilding_throwsConflictException() {
        Mockito.when(addressRepository.findById(1L)).thenReturn(Optional.of(address));
        Mockito.when(buildingRepository.existsBuildingByAddressId(1L)).thenReturn(true);

        Assertions.assertThatThrownBy(() -> addressService.delete(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("1");

        Mockito.verify(addressRepository, Mockito.never()).deleteById(Mockito.any());
    }

    @Test
    @DisplayName("Delete address with non existing id throws not found")
    void delete_nonExistentId_throwsNotFoundException() {
        Mockito.when(addressRepository.findById(99L)).thenReturn(Optional.empty());

        Assertions.assertThatThrownBy(() -> addressService.delete(99L))
                .isInstanceOf(NotFoundException.class);

        Mockito.verify(addressRepository, Mockito.never()).deleteById(Mockito.any());
        Mockito.verify(buildingRepository, Mockito.never()).existsBuildingByAddressId(Mockito.any());
    }
}
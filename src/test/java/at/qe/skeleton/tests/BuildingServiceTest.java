package at.qe.skeleton.tests;

import at.qe.skeleton.common.exceptions.NotFoundException;
import at.qe.skeleton.dtos.BuildingUpdateDTO;
import at.qe.skeleton.models.Address;
import at.qe.skeleton.models.Building;
import at.qe.skeleton.models.Room;
import at.qe.skeleton.repositories.BuildingRepository;
import at.qe.skeleton.repositories.RoomRepository;
import at.qe.skeleton.services.AddressService;
import at.qe.skeleton.services.BuildingService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@ExtendWith(MockitoExtension.class)
class BuildingServiceTest {

    @Mock
    private BuildingRepository buildingRepository;

    @Mock
    private AddressService addressService;

    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private BuildingService buildingService;

    private Building building;

    @BeforeEach
    void setUp() {
        Address address = new Address();

        building = new Building();
        building.setName("Main Building");
        building.setAddress(address);
    }

    @Test
    @DisplayName("Get all buildings returns all")
    void getAllBuildings_returnsAllBuildings() {
        Mockito.when(buildingRepository.findAll()).thenReturn(List.of(building));

        List<Building> result = buildingService.getAllBuildings();

        Assertions.assertThat(result).containsExactly(building);
        Mockito.verify(buildingRepository).findAll();
    }

    @Test
    @DisplayName("Get all buildings empty returns empty list")
    void getAllBuildings_empty_returnsEmptyList() {
        Mockito.when(buildingRepository.findAll()).thenReturn(List.of());

        List<Building> result = buildingService.getAllBuildings();

        Assertions.assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Get building by id returns building")
    void getBuildingById_existingId_returnsBuilding() {
        Mockito.when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));

        Building result = buildingService.getBuildingById(1L);

        Assertions.assertThat(result).isEqualTo(building);
    }

    @Test
    @DisplayName("Get building by id throws when not found")
    void getBuildingById_nonExistingId_throwsNotFoundException() {
        Mockito.when(buildingRepository.findById(99L)).thenReturn(Optional.empty());

        Assertions.assertThatThrownBy(() -> buildingService.getBuildingById(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("Create building with valid data saves and returns")
    void create_validBuilding_savesAndReturns() {
        Mockito.when(buildingRepository.save(building)).thenReturn(building);

        Building result = buildingService.create(building);

        Assertions.assertThat(result).isEqualTo(building);
        Mockito.verify(buildingRepository).save(building);
    }

    @Test
    @DisplayName("Create building without address saves successfully")
    void create_buildingWithoutAddress_savesSuccessfully() {
        building.setAddress(null);
        Mockito.when(buildingRepository.save(building)).thenReturn(building);

        Building result = buildingService.create(building);

        Assertions.assertThat(result).isEqualTo(building);
    }

    @Test
    @DisplayName("Update building with both fields updates name and address")
    void update_bothFieldsProvided_updatesBothFields() {
        Address newAddress = new Address();

        BuildingUpdateDTO dto = new BuildingUpdateDTO("New Name", 2L);

        Mockito.when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));
        Mockito.when(addressService.getById(2L)).thenReturn(newAddress);
        Mockito.when(buildingRepository.save(building)).thenReturn(building);

        Building result = buildingService.update(1L, dto);

        Assertions.assertThat(result.getName()).isEqualTo("New Name");
        Assertions.assertThat(result.getAddress()).isEqualTo(newAddress);
        Mockito.verify(buildingRepository).save(building);
    }

    @Test
    @DisplayName("Update building with only name updates name only")
    void update_onlyNameProvided_updatesNameOnly() {
        BuildingUpdateDTO dto = new BuildingUpdateDTO("Renamed", null);

        Mockito.when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));
        Mockito.when(buildingRepository.save(building)).thenReturn(building);

        Address originalAddress = building.getAddress();

        buildingService.update(1L, dto);

        Assertions.assertThat(building.getName()).isEqualTo("Renamed");
        Assertions.assertThat(building.getAddress()).isEqualTo(originalAddress);
        Mockito.verify(addressService, Mockito.never()).getById(Mockito.any());
    }

    @Test
    @DisplayName("Update building with only address updates address only")
    void update_onlyAddressIdProvided_updatesAddressOnly() {
        Address newAddress = new Address();

        BuildingUpdateDTO dto = new BuildingUpdateDTO(null, 2L);

        Mockito.when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));
        Mockito.when(addressService.getById(2L)).thenReturn(newAddress);
        Mockito.when(buildingRepository.save(building)).thenReturn(building);

        String originalName = building.getName();

        buildingService.update(1L, dto);

        Assertions.assertThat(building.getName()).isEqualTo(originalName);
        Assertions.assertThat(building.getAddress()).isEqualTo(newAddress);
    }

    @Test
    @DisplayName("Update building with non existing id throws not found")
    void update_nonExistingId_throwsNotFoundException() {
        Mockito.when(buildingRepository.findById(99L)).thenReturn(Optional.empty());

        BuildingUpdateDTO dto = new BuildingUpdateDTO("X", null);

        Assertions.assertThatThrownBy(() -> buildingService.update(99L, dto))
                .isInstanceOf(NotFoundException.class);

        Mockito.verify(buildingRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    @DisplayName("Delete building with no rooms deletes directly")
    void delete_buildingWithNoRooms_deletesDirectly() {
        Mockito.when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));

        buildingService.delete(1L);

        Mockito.verify(roomRepository, Mockito.never()).save(Mockito.any());
        Mockito.verify(buildingRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Delete building with rooms nullifies room references before deleting")
    void delete_buildingWithRooms_nullifiesRoomBuildingRefBeforeDeleting() {
        Room room1 = new Room();
        room1.setBuilding(building);

        Room room2 = new Room();
        room2.setBuilding(building);

        building.setRooms(new ArrayList<>(List.of(room1, room2)));

        Mockito.when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));

        buildingService.delete(1L);

        Assertions.assertThat(room1.getBuilding()).isNull();
        Assertions.assertThat(room2.getBuilding()).isNull();
        Assertions.assertThat(building.getRooms()).isEmpty();
        Mockito.verify(roomRepository, Mockito.times(2)).save(Mockito.any(Room.class));
        Mockito.verify(buildingRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Delete building with non existing id throws not found")
    void delete_nonExistingId_throwsNotFoundException() {
        Mockito.when(buildingRepository.findById(99L)).thenReturn(Optional.empty());

        Assertions.assertThatThrownBy(() -> buildingService.delete(99L))
                .isInstanceOf(NotFoundException.class);

        Mockito.verify(buildingRepository, Mockito.never()).deleteById(Mockito.any());
        Mockito.verify(roomRepository, Mockito.never()).save(Mockito.any());
    }
}
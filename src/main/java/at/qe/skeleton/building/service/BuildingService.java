package at.qe.skeleton.building.service;

import at.qe.skeleton.building.dto.BuildingUpdateDTO;
import at.qe.skeleton.common.exceptions.BuildingNotFoundException;
import at.qe.skeleton.building.model.Building;
import at.qe.skeleton.room.model.Room;
import at.qe.skeleton.building.repository.BuildingRepository;
import at.qe.skeleton.room.repository.RoomRepository;
import at.qe.skeleton.address.service.AddressService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BuildingService {

    private final BuildingRepository buildingRepository;
    private final AddressService addressService;
    private final RoomRepository roomRepository;

    public BuildingService(BuildingRepository buildingRepository, AddressService addressService, RoomRepository roomRepository) {
        this.buildingRepository = buildingRepository;
        this.addressService = addressService;
        this.roomRepository = roomRepository;
    }

    public List<Building> getAllBuildings() {
        return buildingRepository.findAll();
    }

    public Building getBuildingById(long id) {
        return buildingRepository.findById(id)
                .orElseThrow(() -> new BuildingNotFoundException("Building with id " + id + " not found"));
    }

    public Building create(Building building) {
        return buildingRepository.save(building);
    }

    public Building update(Long id, BuildingUpdateDTO dto) {
        Building building = getBuildingById(id);

        if (dto.name() != null) {
            building.setName(dto.name());
        }

        if (dto.addressId() != null) {
            building.setAddress(addressService.getById(dto.addressId()));
        }

        return buildingRepository.save(building);
    }

    @Transactional
    public void delete(Long id) {
        Building building = getBuildingById(id);

        for (Room room : building.getRooms()) {
            room.setBuilding(null);
            roomRepository.save(room);
        }
        building.getRooms().clear();

        buildingRepository.deleteById(id);
    }

}

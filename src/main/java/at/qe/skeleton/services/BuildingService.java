package at.qe.skeleton.services;

import at.qe.skeleton.dtos.BuildingUpdateDTO;
import at.qe.skeleton.exceptions.BuildingNotFoundException;
import at.qe.skeleton.model.Building;
import at.qe.skeleton.model.Room;
import at.qe.skeleton.repositories.BuildingRepository;
import at.qe.skeleton.repositories.RoomRepository;
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

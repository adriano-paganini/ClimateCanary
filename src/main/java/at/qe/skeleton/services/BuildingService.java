package at.qe.skeleton.services;

import at.qe.skeleton.dtos.BuildingUpdateDTO;
import at.qe.skeleton.exceptions.BuildingNotFoundException;
import at.qe.skeleton.model.Building;
import at.qe.skeleton.model.Room;
import at.qe.skeleton.repositories.BuildingRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BuildingService {

    private final BuildingRepository buildingRepository;

    public BuildingService(BuildingRepository buildingRepository) {
        this.buildingRepository = buildingRepository;
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

        // TODO: if (dto.addressId() != null) resolve and set address

        return buildingRepository.save(building);
    }

    public void delete(Long id) {

        Building building = getBuildingById(id);

        for (Room room : building.getRooms()) {
            room.setBuilding(null);
        }

        building.getRooms().clear();
        buildingRepository.deleteById(id);
    }

}

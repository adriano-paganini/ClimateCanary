package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.BuildingCreateDTO;
import at.qe.skeleton.model.Building;
import org.springframework.stereotype.Service;

@Service
public class BuildingCreateMapper implements DTOMapper<Building, BuildingCreateDTO> {

    @Override
    public BuildingCreateDTO mapTo(Building entity) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Building mapFrom(BuildingCreateDTO dto) {
        Building building = new Building();
        building.setName(dto.name());

        // TODO: Address missing

        return building;
    }
}

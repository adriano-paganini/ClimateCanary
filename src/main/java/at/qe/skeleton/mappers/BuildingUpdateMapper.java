package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.BuildingUpdateDTO;
import at.qe.skeleton.model.Building;
import org.springframework.stereotype.Service;

@Service
public class BuildingUpdateMapper implements DTOMapper<Building, BuildingUpdateDTO>{

    @Override
    public BuildingUpdateDTO mapTo(Building entity) {
        throw new  UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Building mapFrom(BuildingUpdateDTO dto) {
        Building building = new Building();
        building.setName(dto.name());

        // TODO: Address

        return building;
    }
}

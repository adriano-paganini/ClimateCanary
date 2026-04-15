package at.qe.skeleton.building.mapper;

import at.qe.skeleton.building.dto.BuildingDTO;
import at.qe.skeleton.common.DTOMapper;
import at.qe.skeleton.building.model.Building;
import org.springframework.stereotype.Service;

@Service
public class BuildingMapper implements DTOMapper<Building, BuildingDTO> {

    @Override
    public BuildingDTO mapTo(Building entity) {
        return new BuildingDTO(
                entity.getId(),
                entity.getName(),
                entity.getAddress() != null ? entity.getAddress().getId() : null
        );
    }

    @Override
    public Building mapFrom(BuildingDTO dto) {

        Building building = new Building();
        building.setName(dto.name());

        return building;
    }
}

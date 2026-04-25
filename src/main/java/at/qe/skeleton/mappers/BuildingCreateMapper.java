package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.BuildingCreateDTO;
import at.qe.skeleton.common.DTOMapper;
import at.qe.skeleton.models.Building;
import at.qe.skeleton.services.AddressService;
import org.springframework.stereotype.Service;

@Service
public class BuildingCreateMapper implements DTOMapper<Building, BuildingCreateDTO> {

    private final AddressService addressService;

    public BuildingCreateMapper(AddressService addressService) {
        this.addressService = addressService;
    }

    @Override
    public BuildingCreateDTO mapTo(Building entity) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Building mapFrom(BuildingCreateDTO dto) {
        Building building = new Building();
        building.setName(dto.name());
        building.setAddress(addressService.getById(dto.addressId()));
        return building;
    }
}

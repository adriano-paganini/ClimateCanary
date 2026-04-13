package at.qe.skeleton.building.mapper;

import at.qe.skeleton.building.dto.BuildingCreateDTO;
import at.qe.skeleton.common.DTOMapper;
import at.qe.skeleton.building.model.Building;
import at.qe.skeleton.address.service.AddressService;
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

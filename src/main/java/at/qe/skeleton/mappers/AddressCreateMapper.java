package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.AddressCreateDTO;
import at.qe.skeleton.common.DTOMapper;
import at.qe.skeleton.models.Address;
import org.springframework.stereotype.Service;

@Service
public class AddressCreateMapper implements DTOMapper<Address, AddressCreateDTO> {

    @Override
    public AddressCreateDTO mapTo(Address entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Address mapFrom(AddressCreateDTO dto) {
        Address address = new Address();
        address.setCountry(dto.country());
        address.setZipCode(dto.zipCode());
        address.setCity(dto.city());
        address.setStreet(dto.street());
        address.setHouseNumber(dto.houseNumber());
        address.setExtra(dto.extra());
        return address;
    }
}
package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.AddressDTO;
import at.qe.skeleton.common.DTOMapper;
import at.qe.skeleton.models.Address;
import org.springframework.stereotype.Service;

@Service
public class AddressMapper implements DTOMapper<Address, AddressDTO> {

    @Override
    public AddressDTO mapTo(Address entity) {
        return new AddressDTO(
                entity.getId(),
                entity.getCountry(),
                entity.getZipCode(),
                entity.getCity(),
                entity.getStreet(),
                entity.getHouseNumber(),
                entity.getExtra()
        );
    }

    @Override
    public Address mapFrom(AddressDTO dto) {
        throw new UnsupportedOperationException();
    }
}
